package com.protoano.granules.crystal;

import com.protoano.granules.sound.GranulesSounds;
import java.util.Comparator;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CrystalDefense {
    public static final TagKey<Block> ANCHORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("granules", "crystal_anchors"));
    public static final TagKey<Block> OBSIDIANS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("granules", "obsidians"));
    public static final TagKey<EntityType<?>> TARGETS = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("granules", "crystal_aerial_targets"));

    private boolean enabled;
    private boolean ghasts;
    private UUID playerTarget;
    private UUID owner;
    private LivingEntity target;
    private int attackTicks;

    public static void initialize() {
        UseBlockCallback.EVENT.register(CrystalDefense::place);
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof EndCrystal crystal) {
                return ((CrystalDefenseAccess) crystal).granules$defense().configure(crystal, player, hand);
            }
            return InteractionResult.PASS;
        });
    }

    public void enable(EndCrystal crystal) {
        enabled = true;
        ((CrystalDefenseAccess) crystal).granules$setDefense(true);
    }

    public boolean enabled() {
        return enabled;
    }

    public static InteractionResult place(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.CRYSTAL_DEFENSE)) {
            return InteractionResult.PASS;
        }
        var stack = player.getItemInHand(hand);
        BlockPos anchor = hit.getBlockPos();
        if (!stack.is(Items.END_CRYSTAL) || !level.getBlockState(anchor).is(ANCHORS)) {
            return InteractionResult.PASS;
        }
        if (player.isSpectator() || !player.isShiftKeyDown() || hit.getDirection() != Direction.UP
            || !player.mayUseItemAt(anchor.above(), hit.getDirection(), stack) || !level.mayInteract(player, anchor)) {
            return InteractionResult.FAIL;
        }
        if (pyramidTier(level, anchor) == 0 || !level.isEmptyBlock(anchor.above()) || !level.isEmptyBlock(anchor.above(2))
            || !level.getEntities(null, new AABB(anchor.above()).expandTowards(0, 1, 0)).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            EndCrystal crystal = new EndCrystal(level, anchor.getX() + 0.5, anchor.getY() + 1.0, anchor.getZ() + 0.5);
            ((CrystalDefenseAccess) crystal).granules$defense().enable(crystal);
            ((CrystalDefenseAccess) crystal).granules$defense().owner = player.getUUID();
            crystal.setShowBottom(false);
            if (!server.addFreshEntity(crystal)) {
                return InteractionResult.FAIL;
            }
            server.gameEvent(player, GameEvent.ENTITY_PLACE, anchor.above());
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                recordPower(serverPlayer, pyramidTier(level, anchor));
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static int pyramidTier(Level level, BlockPos anchor) {
        if (!level.getBlockState(anchor).is(ANCHORS)) {
            return 0;
        }
        int tier = 0;
        for (int depth = 1; depth <= 4; depth++) {
            for (int x = -depth; x <= depth; x++) {
                for (int z = -depth; z <= depth; z++) {
                    BlockPos pos = anchor.offset(x, -depth, z);
                    if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(OBSIDIANS)) {
                        return tier;
                    }
                }
            }
            tier = depth;
        }
        return tier;
    }

    public static int range(int tier) {
        return tier == 0 ? 0 : 10 + tier * 10;
    }

    private static void recordPower(net.minecraft.server.level.ServerPlayer player, int tier) {
        if (tier > 0) {
            com.protoano.granules.advancement.GranulesAdvancements.award(player, "anti_air_alternatives");
        }
        if (tier == 4) {
            com.protoano.granules.advancement.GranulesAdvancements.award(player, "ender_sender");
        }
    }

    public InteractionResult configure(EndCrystal crystal, Player player, InteractionHand hand) {
        if (!((CrystalDefenseAccess) crystal).granules$isDefense() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        var stack = player.getItemInHand(hand);
        if (stack.is(Items.PLAYER_HEAD)) {
            boolean accepted = com.protoano.granules.entity.PlayerHeadFilter.resolve(stack, player, selectedPlayer -> {
                if (crystal.isRemoved()) {
                    return;
                }
                boolean removed = selectedPlayer.equals(playerTarget);
                playerTarget = removed ? null : selectedPlayer;
                target = null;
                attackTicks = 0;
                crystal.setBeamTarget(null);
                filterParticles((ServerLevel) crystal.level(), crystal, removed);
            });
            return accepted ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else if (!stack.is(Items.GHAST_TEAR)) {
            return InteractionResult.PASS;
        }
        if (crystal.level() instanceof ServerLevel server) {
            ghasts = !ghasts;
            boolean removed = !ghasts;
            target = null;
            attackTicks = 0;
            crystal.setBeamTarget(null);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            filterParticles(server, crystal, removed);
        }
        return InteractionResult.SUCCESS;
    }

    private static void filterParticles(ServerLevel level, EndCrystal crystal, boolean removed) {
        Vec3 center = crystal.position().add(0, 1, 0);
        for (int index = 0; index < 48; index++) {
            double angle = index * Math.PI * 2.0 / 48.0;
            Vec3 radial = new Vec3(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
            Vec3 destination = removed ? center.add(radial) : center;
            Vec3 offset = removed ? radial.scale(-1) : radial;
            level.sendParticles(ParticleTypes.ENCHANT, destination.x, destination.y, destination.z, 0, offset.x, offset.y, offset.z, 1.0);
        }
        level.playSound(null, crystal.blockPosition(), (removed ? GranulesSounds.FILTER_REMOVE : GranulesSounds.FILTER_APPLY), SoundSource.BLOCKS, 1.0F, removed ? 0.8F : 1.0F);
    }

    public boolean matches(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        if (candidate instanceof Player player) {
            return !player.getAbilities().instabuild && player.getUUID().equals(playerTarget);
        }
        if (candidate.getType() == EntityTypes.GHAST) {
            return ghasts;
        }
        return candidate.getType().builtInRegistryHolder().is(TARGETS);
    }

    public void tick(EndCrystal crystal) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.CRYSTAL_DEFENSE)) {
            crystal.setBeamTarget(null);
            target = null;
            return;
        }
        if (!enabled || !(crystal.level() instanceof ServerLevel level)) {
            return;
        }
        int tier = pyramidTier(level, crystal.blockPosition().below());
        int radius = range(tier);
        if (tier > 0 && crystal.tickCount % 20 == 0) {
            if (owner != null) {
                var player = level.getServer().getPlayerList().getPlayer(owner);
                if (player != null) {
                    recordPower(player, tier);
                }
            }
            for (var player : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                crystal.getBoundingBox().inflate(10), player -> !player.isSpectator())) {
                recordPower(player, tier);
            }
        }
        if (radius == 0) {
            target = null;
            attackTicks = 0;
            crystal.setBeamTarget(null);
            return;
        }
        if (target != null && !canTarget(crystal, target, radius)) {
            target = null;
            attackTicks = 0;
        }
        if (target == null && crystal.tickCount % 10 == 0) {
            target = level.getEntitiesOfClass(LivingEntity.class, crystal.getBoundingBox().inflate(radius),
                candidate -> canTarget(crystal, candidate, radius)).stream()
                .min(Comparator.comparingDouble(crystal::distanceToSqr)).orElse(null);
        }
        crystal.setBeamTarget(target == null ? null : BlockPos.containing(target.getBoundingBox().getCenter()));
        if (target != null && ++attackTicks >= com.protoano.granules.config.BalanceConfig.Setting.CRYSTAL_ATTACK_TICKS.intValue()) {
            target.hurtServer(level, level.damageSources().indirectMagic(crystal, crystal), com.protoano.granules.config.BalanceConfig.Setting.CRYSTAL_DAMAGE.floatValue());
            attackTicks = 0;
        }
    }

    private boolean canTarget(EndCrystal crystal, LivingEntity candidate, int radius) {
        if (candidate.level() != crystal.level() || !matches(candidate) || crystal.distanceToSqr(candidate) > radius * radius) {
            return false;
        }
        Vec3 start = crystal.position().add(0, 1, 0);
        Vec3 end = candidate.getBoundingBox().getCenter();
        return crystal.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, crystal)).getType() == HitResult.Type.MISS;
    }

    public void save(ValueOutput output) {
        output.putBoolean("GranulesCrystalDefense", enabled);
        if (owner != null) {
            output.putString("GranulesCrystalOwner", owner.toString());
        }
        output.putBoolean("GranulesCrystalGhasts", ghasts);
        if (playerTarget != null) {
            output.putString("GranulesCrystalPlayer", playerTarget.toString());
        }
    }

    public void load(ValueInput input) {
        enabled = input.getBooleanOr("GranulesCrystalDefense", false);
        owner = input.getString("GranulesCrystalOwner").map(value -> {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }).orElse(null);
        ghasts = input.getBooleanOr("GranulesCrystalGhasts", false);
        playerTarget = input.getString("GranulesCrystalPlayer").map(value -> {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }).orElse(null);
        target = null;
        attackTicks = 0;
    }
}
