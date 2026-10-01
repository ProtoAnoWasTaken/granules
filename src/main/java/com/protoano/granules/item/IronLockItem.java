package com.protoano.granules.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class IronLockItem extends Item {
    public static final BooleanProperty INVERTED = BooleanProperty.create("granules_inverted");

    public IronLockItem(Properties properties) {
        super(properties);
    }

    public static boolean supports(Block block) {
        String id = block.getDescriptionId();
        return id.equals("block.minecraft.iron_door") || id.equals("block.minecraft.iron_trapdoor");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if ((!state.hasProperty(INVERTED) && !state.hasProperty(StateLocks.LOCKED)) || player == null || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (state.hasProperty(StateLocks.LOCKED)) {
                StateLocks.toggle(level, pos, state);
                com.protoano.granules.advancement.GranulesAdvancements.recordLock(player, !StateLocks.locked(state));
                setModel(context.getItemInHand(), StateLocks.locked(state));
                context.getItemInHand().hurtAndBreak(1, player, context.getHand());
                return InteractionResult.SUCCESS;
            }
            update(level, pos, state, !state.getValue(INVERTED), player);
            com.protoano.granules.advancement.GranulesAdvancements.recordLock(player, !state.getValue(INVERTED));
            setModel(context.getItemInHand(), state.getValue(INVERTED));
            context.getItemInHand().hurtAndBreak(1, player, context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    public static void update(Level level, BlockPos pos, BlockState state, boolean inverted, Entity source) {
        if (state.is(Blocks.IRON_DOOR) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        if (!state.hasProperty(INVERTED)) {
            return;
        }
        boolean door = state.is(Blocks.IRON_DOOR);
        boolean powered = level.hasNeighborSignal(pos) || door && level.hasNeighborSignal(pos.above());
        boolean open = powered != inverted;
        boolean changed = state.getValue(BlockStateProperties.OPEN) != open;
        BlockState updated = state.setValue(INVERTED, inverted).setValue(BlockStateProperties.POWERED, powered).setValue(BlockStateProperties.OPEN, open);
        level.setBlock(pos, updated, 2);
        if (door) {
            BlockState upper = level.getBlockState(pos.above());
            if (upper.is(Blocks.IRON_DOOR)) {
                level.setBlock(pos.above(), upper.setValue(INVERTED, inverted).setValue(BlockStateProperties.POWERED, powered).setValue(BlockStateProperties.OPEN, open), 2);
            }
        }
        if (changed) {
            var sound = door ? (open ? BlockSetType.IRON.doorOpen() : BlockSetType.IRON.doorClose())
                : (open ? BlockSetType.IRON.trapdoorOpen() : BlockSetType.IRON.trapdoorClose());
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(source, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        boolean openModel = false;
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            var start = player.getEyePosition();
            var end = start.add(player.getViewVector(1.0F).scale(player.entityInteractionRange()));
            var entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0),
                target -> !target.isSpectator() && target.isPickable(), Math.min(start.distanceToSqr(hit.getLocation()), start.distanceToSqr(end)));
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                var state = level.getBlockState(blockHit.getBlockPos());
                openModel = state.hasProperty(INVERTED) && !state.getValue(INVERTED);
                openModel |= state.hasProperty(StateLocks.LOCKED) && !StateLocks.locked(state);
            }
            if (entityHit != null) {
                openModel = entityHit.getEntity() instanceof net.minecraft.world.entity.decoration.painting.Painting painting
                    && !painting.entityTags().contains(PaintingLocks.LOCKED);
                openModel |= EntityLocks.supports(entityHit.getEntity()) && !EntityLocks.locked(entityHit.getEntity());
            }
        }
        if (setModel(stack, openModel)) {
            player.inventoryMenu.broadcastChanges();
        }
    }

    private static boolean setModel(ItemStack stack, boolean open) {
        CustomModelData model = new CustomModelData(List.of(), List.of(), List.of(open ? "open" : "closed"), List.of());
        if (model.equals(stack.get(DataComponents.CUSTOM_MODEL_DATA))) {
            return false;
        }
        stack.set(DataComponents.CUSTOM_MODEL_DATA, model);
        return true;
    }
}
