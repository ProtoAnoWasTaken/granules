package com.protoano.granules.grave;

import com.protoano.granules.mixin.BuriedItemAccessor;
import com.protoano.granules.world.ArchaeologyContent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GraveShovelItem extends ShovelItem {
    private static final Map<UUID, Burial> SESSIONS = new HashMap<>();

    public GraveShovelItem(Properties properties) {
        super(ToolMaterial.IRON, 1.5F, -3.0F, properties);
    }

    public static Block suspicious(BlockState state) {
        if (state.is(Blocks.SAND)) {
            return Blocks.SUSPICIOUS_SAND;
        }
        if (state.is(Blocks.RED_SAND)) {
            return ArchaeologyContent.SUSPICIOUS_RED_SAND;
        }
        if (state.is(Blocks.GRAVEL)) {
            return Blocks.SUSPICIOUS_GRAVEL;
        }
        return null;
    }

    private static InteractionHand otherHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player != null) {
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().scale(player.blockInteractionRange() + 2));
            for (var ghost : level.getEntitiesOfClass(GraveGhostEntity.class, player.getBoundingBox().inflate(6))) {
                if (ghost.visibleTo(player) && ghost.getBoundingBox().inflate(0.3).clip(start, end).isPresent()) {
                    if (!(player instanceof ServerPlayer serverPlayer) || GraveRescue.contact(serverPlayer, ghost, context.getHand())) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        if (player == null || suspicious(level.getBlockState(pos)) == null) {
            return super.useOn(context);
        }
        ItemStack buried = player.getItemInHand(otherHand(context.getHand()));
        if (buried.isEmpty() || !player.mayBuild() || !level.mayInteract(player, pos)
            || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            removeSession(player.getUUID());
            var preview = new net.minecraft.world.entity.Display.ItemDisplay(GraveContent.BURIAL, server);
            preview.setItemStack(buried.copyWithCount(1));
            preview.setItemTransform(net.minecraft.world.item.ItemDisplayContext.GROUND);
            preview.setPos(pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5);
            server.addFreshEntity(preview);
            SESSIONS.put(player.getUUID(), new Burial(server, pos.immutable(), level.getBlockState(pos), buried.copyWithCount(1), context.getHand(), preview));
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 80;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack shovel, int remaining) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (GraveRescue.isPulling(player)) {
            return;
        }
        Burial burial = SESSIONS.get(player.getUUID());
        HitResult hit = player.pick(player.blockInteractionRange(), 1, false);
        if (burial == null || burial.level != level || !(hit instanceof BlockHitResult blockHit)
            || hit.getType() != HitResult.Type.BLOCK || !blockHit.getBlockPos().equals(burial.pos)
            || !level.getBlockState(burial.pos).equals(burial.state)
            || !ItemStack.isSameItemSameComponents(player.getItemInHand(otherHand(burial.hand)), burial.item)
            || player.getItemInHand(otherHand(burial.hand)).isEmpty() || !player.mayBuild()
            || !level.mayInteract(player, burial.pos)) {
            removeSession(player.getUUID());
            player.stopUsingItem();
            return;
        }
        int elapsed = getUseDuration(shovel, player) - remaining + 1;
        if (elapsed % 10 != 5) {
            return;
        }
        burial.level.playSound(null, burial.pos, burial.state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1);
        int stage = (elapsed - 5) / 10 + 1;
        burial.preview.setPos(burial.pos.getX() + 0.5, burial.pos.getY() + 1.15 - stage * 0.15, burial.pos.getZ() + 0.5);
        burial.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, burial.state),
            blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z, 8, 0.15, 0.05, 0.15, 0.01);
        if (elapsed < 35) {
            return;
        }
        bury(player, burial.pos, burial.hand);
        removeSession(player.getUUID());
        player.stopUsingItem();
    }

    public static boolean bury(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        ServerLevel level = player.level();
        Block block = suspicious(level.getBlockState(pos));
        ItemStack item = player.getItemInHand(otherHand(hand));
        if (block == null || item.isEmpty() || !player.getItemInHand(hand).is(GraveContent.SHOVEL)
            || !player.mayBuild() || !level.mayInteract(player, pos)
            || !player.mayUseItemAt(pos, Direction.UP, player.getItemInHand(hand))) {
            return false;
        }
        if (!level.setBlockAndUpdate(pos, block.defaultBlockState())) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
            ((BuriedItemAccessor) brushable).granules$setBuriedItem(item.copyWithCount(1));
            brushable.setChanged();
            level.sendBlockUpdated(pos, brushable.getBlockState(), brushable.getBlockState(), Block.UPDATE_CLIENTS);
            item.consume(1, player);
            com.protoano.granules.advancement.GranulesAdvancements.award(player, "something_suspicious");
            player.getItemInHand(hand).hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return true;
        }
        return false;
    }

    public static void cleanSessions(MinecraftServer server) {
        SESSIONS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isUsingItem() || !player.getUseItem().is(GraveContent.SHOVEL)
                || player.level() != entry.getValue().level) {
                entry.getValue().preview.discard();
                return true;
            }
            return false;
        });
    }

    public static void clearSessions() {
        for (Burial burial : SESSIONS.values()) {
            burial.preview.discard();
        }
        SESSIONS.clear();
    }

    private static void removeSession(UUID player) {
        Burial burial = SESSIONS.remove(player);
        if (burial != null) {
            burial.preview.discard();
        }
    }

    private record Burial(ServerLevel level, BlockPos pos, BlockState state, ItemStack item, InteractionHand hand,
        net.minecraft.world.entity.Display.ItemDisplay preview) {
    }
}
