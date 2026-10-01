package com.protoano.granules.grave;

import com.protoano.granules.pet.PetBedService;
import com.protoano.granules.rabbit.RabbitHoleBlock;
import com.protoano.granules.rabbit.RabbitHoleBlockEntity;
import com.protoano.granules.rabbit.RabbitHoleContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GraveRescue {
    public static final double RANGE = 32;
    private static final Map<UUID, List<GraveGhostEntity>> GHOSTS = new HashMap<>();
    private static final Map<UUID, Request> REQUESTS = new HashMap<>();
    public static final int PULL_TICKS = 40;
    private static final Map<UUID, Pull> PULLS = new HashMap<>();

    private GraveRescue() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(GraveRescue::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GHOSTS.clear();
            REQUESTS.clear();
            PULLS.clear();
            GraveShovelItem.clearSessions();
            GraveDiscovery.clear();
        });
    }

    public static boolean holdingShovel(Player player) {
        return !com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.GRAVE_SHOVEL)
            && player.isAlive() && !player.isSpectator()
            && (player.getMainHandItem().is(GraveContent.SHOVEL) || player.getOffhandItem().is(GraveContent.SHOVEL));
    }

    public static Vec3 project(LivingEntity target, Level destination) {
        double scale = DimensionType.getTeleportationScale(target.level().dimensionType(), destination.dimensionType());
        return new Vec3(target.getX() * scale, target.getY(), target.getZ() * scale);
    }

    public static boolean canProject(ServerPlayer viewer, LivingEntity target) {
        return holdingShovel(viewer) && target.isAlive() && !target.isRemoved() && !target.isSpectator()
            && target.level() != viewer.level() && !(target instanceof GraveGhostEntity)
            && (target instanceof ServerPlayer || PetBedService.isOwnedBy(target, viewer.getUUID()))
            && horizontalDistanceSquared(project(target, viewer.level()), viewer.position())
                <= Math.pow(com.protoano.granules.config.BalanceConfig.Setting.GRAVE_GHOST_RADIUS.value(), 2);
    }

    public static double horizontalDistanceSquared(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return x * x + z * z;
    }

    public static Vec3 groundPosition(LivingEntity target, ServerLevel destination, ServerPlayer viewer) {
        Vec3 projected = project(target, destination);
        int center = (int) Math.floor(viewer.getY()) - 1;
        for (int distance = 0; distance <= 32; distance++) {
            for (int direction : new int[] {-1, 1}) {
                if (direction == 1 && (distance == 0 || distance > 8)) {
                    continue;
                }
                int y = center + distance * direction;
                BlockPos ground = BlockPos.containing(projected.x, y, projected.z);
                if (!destination.isInWorldBounds(ground) || !destination.hasChunkAt(ground)) {
                    continue;
                }
                if (destination.getBlockState(ground).isFaceSturdy(destination, ground, Direction.UP)
                    && destination.getBlockState(ground.above()).getCollisionShape(destination, ground.above()).isEmpty()
                    && destination.getBlockState(ground.above(2)).getCollisionShape(destination, ground.above(2)).isEmpty()) {
                    return new Vec3(projected.x, y + 1, projected.z);
                }
            }
        }
        return new Vec3(projected.x, viewer.getY(), projected.z);
    }

    public static Vec3 ghostPosition(LivingEntity target, ServerLevel destination, ServerPlayer viewer) {
        return groundPosition(target, destination, viewer).add(0, -2, 0);
    }

    public static Vec3 animatedGhostPosition(LivingEntity target, ServerLevel destination, ServerPlayer viewer) {
        Vec3 buried = ghostPosition(target, destination, viewer);
        Pull pull = PULLS.get(target.getUUID());
        if (pull == null || !pull.rescuer.equals(viewer.getUUID())) {
            return buried;
        }
        double progress = Math.clamp((destination.getGameTime() - pull.started) / (double) PULL_TICKS, 0, 1);
        double smooth = progress * progress * (3 - 2 * progress);
        return buried.add(0, 2 * smooth, 0);
    }

    public static boolean isPulling(ServerPlayer player) {
        return PULLS.values().stream().anyMatch(pull -> pull.rescuer.equals(player.getUUID()));
    }

    private static boolean beginPull(ServerPlayer rescuer, LivingEntity target, InteractionHand hand) {
        Pull existing = PULLS.get(target.getUUID());
        if (existing != null) {
            return existing.rescuer.equals(rescuer.getUUID());
        }
        if (isPulling(rescuer) || target.isPassenger() || target.isVehicle()) {
            return false;
        }
        if (safeHole((ServerLevel) target.level(), target.position(), target) == null
            || safeHole(rescuer.level(), groundPosition(target, rescuer.level(), rescuer), target) == null) {
            rescuer.sendOverlayMessage(Component.translatable("message.granules.grave_blocked"));
            return false;
        }
        PULLS.put(target.getUUID(), new Pull(rescuer.getUUID(), target, hand, rescuer.level(), rescuer.level().getGameTime()));
        rescuer.startUsingItem(hand);
        return true;
    }

    public static void tickPulls(MinecraftServer server) {
        PULLS.entrySet().removeIf(entry -> {
            Pull pull = entry.getValue();
            ServerPlayer rescuer = server.getPlayerList().getPlayer(pull.rescuer);
            boolean valid = rescuer != null && rescuer.level() == pull.level
                && rescuer.isUsingItem() && rescuer.getUsedItemHand() == pull.hand
                && rescuer.getItemInHand(pull.hand).is(GraveContent.SHOVEL)
                && withinContact(rescuer, pull.target) && consenting(pull.target, rescuer);
            if (!valid) {
                if (rescuer != null && rescuer.getUseItem().is(GraveContent.SHOVEL)) {
                    rescuer.stopUsingItem();
                }
                return true;
            }
            long elapsed = pull.level.getGameTime() - pull.started;
            BlockPos ground = BlockPos.containing(groundPosition(pull.target, pull.level, rescuer)).below();
            var state = pull.level.getBlockState(ground);
            if (elapsed % 10 == 5) {
                pull.level.playSound(null, ground, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1);
                pull.level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state),
                    ground.getX() + 0.5, ground.getY() + 1.05, ground.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.03);
            }
            if (elapsed < PULL_TICKS) {
                return false;
            }
            rescue(rescuer, pull.target, pull.hand);
            rescuer.stopUsingItem();
            return true;
        });
    }

    public static boolean consenting(LivingEntity target, ServerPlayer rescuer) {
        if (target instanceof ServerPlayer player) {
            return player.isShiftKeyDown() && player.getXRot() >= 75;
        }
        return PetBedService.isOwnedBy(target, rescuer.getUUID());
    }

    public static boolean contact(ServerPlayer player, GraveGhostEntity ghost, InteractionHand hand) {
        LivingEntity target = ghost.target;
        if (!ghost.visibleTo(player) || target == null || !player.getItemInHand(hand).is(GraveContent.SHOVEL)
            || !withinContact(player, target)) {
            return false;
        }
        long now = player.level().getGameTime();
        Request previous = REQUESTS.get(target.getUUID());
        if (target instanceof ServerPlayer other && (previous == null || now - previous.created >= 40)) {
            other.connection.send(new ClientboundSetTitlesAnimationPacket(5, 60, 10));
            other.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.granules.grave_pull")));
            other.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
        }
        if (consenting(target, player)) {
            return beginPull(player, target, hand);
        }
        if (previous == null || now - previous.created >= 40) {
            REQUESTS.put(target.getUUID(), new Request(player.getUUID(), target, hand, now));
        }
        return true;
    }

    private static boolean withinContact(ServerPlayer player, LivingEntity target) {
        return canProject(player, target)
            && new AABB(ghostPosition(target, player.level(), player).add(-0.3, 0, -0.3), ghostPosition(target, player.level(), player).add(0.3, 1.8, 0.3))
                .distanceToSqr(player.getEyePosition()) <= player.entityInteractionRange() * player.entityInteractionRange();
    }

    private static void tick(MinecraftServer server) {
        REQUESTS.entrySet().removeIf(entry -> {
            Request request = entry.getValue();
            ServerPlayer rescuer = server.getPlayerList().getPlayer(request.rescuer);
            if (rescuer == null || rescuer.level().getGameTime() - request.created > 100
                || !rescuer.getItemInHand(request.hand).is(GraveContent.SHOVEL) || !withinContact(rescuer, request.target)) {
                return true;
            }
            if (consenting(request.target, rescuer)) {
                beginPull(rescuer, request.target, request.hand);
                return true;
            }
            return false;
        });
        GraveShovelItem.cleanSessions(server);
        tickPulls(server);
        GraveDiscovery.tick(server);
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        GHOSTS.entrySet().removeIf(entry -> {
            ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
            entry.getValue().removeIf(ghost -> {
                if (viewer == null || ghost.isRemoved() || ghost.level() != viewer.level() || !holdingShovel(viewer)) {
                    ghost.discard();
                    return true;
                }
                return false;
            });
            return viewer == null || !holdingShovel(viewer);
        });
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (!holdingShovel(viewer)) {
                continue;
            }
            List<GraveGhostEntity> ghosts = GHOSTS.computeIfAbsent(viewer.getUUID(), ignored -> new ArrayList<>());
            for (ServerLevel level : server.getAllLevels()) {
                if (level == viewer.level()) {
                    continue;
                }
                for (Entity entity : level.getAllEntities()) {
                    if (!(entity instanceof LivingEntity target) || !canProject(viewer, target)
                        || ghosts.stream().anyMatch(ghost -> ghost.target == target && !ghost.isRemoved())) {
                        continue;
                    }
                    GraveGhostEntity ghost = new GraveGhostEntity(GraveContent.GHOST, viewer.level());
                    ghost.configure(viewer, target);
                    if (viewer.level().addFreshEntity(ghost)) {
                        ghosts.add(ghost);
                    }
                }
            }
        }
    }

    public static boolean rescue(ServerPlayer rescuer, LivingEntity target, InteractionHand hand) {
        if (!withinContact(rescuer, target) || !consenting(target, rescuer)
            || !rescuer.getItemInHand(hand).is(GraveContent.SHOVEL) || target.isPassenger() || target.isVehicle()) {
            return false;
        }
        ServerLevel origin = (ServerLevel) target.level();
        ServerLevel destination = rescuer.level();
        BlockPos from = safeHole(origin, target.position(), target);
        BlockPos to = safeHole(destination, groundPosition(target, destination, rescuer), target);
        if (from == null || to == null || !destination.mayInteract(rescuer, to)) {
            rescuer.sendOverlayMessage(Component.translatable("message.granules.grave_blocked"));
            return false;
        }
        target.setPortalCooldown();
        var emergenceMaterial = destination.getBlockState(to.below());
        Entity arrived = target.teleport(new TeleportTransition(destination, Vec3.atBottomCenterOf(to), Vec3.ZERO,
            target.getYRot(), target.getXRot(), TeleportTransition.DO_NOTHING));
        if (arrived == null) {
            return false;
        }
        arrived.setPortalCooldown();
        origin.setBlockAndUpdate(from, RabbitHoleContent.BLOCK.defaultBlockState().setValue(RabbitHoleBlock.RESCUE, true));
        destination.setBlockAndUpdate(to, RabbitHoleContent.BLOCK.defaultBlockState().setValue(RabbitHoleBlock.RESCUE, true));
        if (origin.getBlockEntity(from) instanceof RabbitHoleBlockEntity first
            && destination.getBlockEntity(to) instanceof RabbitHoleBlockEntity second) {
            first.linkRescue(destination, to);
            second.linkRescue(origin, from);
        }
        origin.playSound(null, from, RabbitHoleContent.ENTER_SOUND, SoundSource.PLAYERS, 1, 1);
        destination.playSound(null, to, RabbitHoleContent.EMERGE_SOUND, SoundSource.PLAYERS, 1, 1);
        destination.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, emergenceMaterial),
            to.getX() + 0.5, to.getY() + 0.2, to.getZ() + 0.5, 60, 0.45, 0.3, 0.45, 0.15);
        rescuer.getItemInHand(hand).hurtAndBreak(1, rescuer, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        com.protoano.granules.advancement.GranulesAdvancements.award(rescuer, "am_i_really_going");
        return true;
    }

    private static BlockPos safeHole(ServerLevel level, Vec3 center, LivingEntity target) {
        BlockPos base = BlockPos.containing(center);
        for (int radius = 0; radius <= 3; radius++) {
            for (int y = -3; y <= 3; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) {
                            continue;
                        }
                        BlockPos pos = base.offset(x, y, z);
                        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                            || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                            || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                            || level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK)
                            || level.getBlockState(pos.below()).is(Blocks.CACTUS)
                            || level.getBlockState(pos.below()).is(Blocks.CAMPFIRE)
                            || level.getBlockState(pos.below()).is(Blocks.SOUL_CAMPFIRE)) {
                            continue;
                        }
                        AABB box = target.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(target.position()));
                        if (level.noCollision(target, box) && !level.containsAnyLiquid(box)) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    private record Request(UUID rescuer, LivingEntity target, InteractionHand hand, long created) {
    }

    private record Pull(UUID rescuer, LivingEntity target, InteractionHand hand, ServerLevel level, long started) {
    }
}
