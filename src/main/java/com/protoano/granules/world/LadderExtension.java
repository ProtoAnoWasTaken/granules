package com.protoano.granules.world;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class LadderExtension {
    private LadderExtension() {
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> extend(player, level, hit));
    }

    public static InteractionResult extend(Player player, Level level, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || player.isSpectator() || !player.mayBuild()
            || hit.getDirection() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState anchor = level.getBlockState(pos);
        if (!anchor.is(Blocks.LADDER) || !anchor.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }
        InteractionHand hand = player.getMainHandItem().is(Items.LADDER)
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.LADDER)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction facing = anchor.getValue(LadderBlock.FACING);
        BlockPos cursor = pos.below();
        while (cursor.getY() >= level.getMinY()) {
            BlockState existing = level.getBlockState(cursor);
            if (existing.is(Blocks.LADDER) && existing.getValue(LadderBlock.FACING) == facing) {
                cursor = cursor.below();
                continue;
            }
            BlockPlaceContext context = new BlockPlaceContext(player, hand, stack,
                new BlockHitResult(Vec3.atCenterOf(cursor), Direction.UP, cursor, false));
            if (!existing.canBeReplaced(context) || !level.mayInteract(player, cursor)
                || !player.mayUseItemAt(cursor, Direction.UP, stack)) {
                return InteractionResult.SUCCESS;
            }
            BlockState ladder = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, facing)
                .setValue(LadderBlock.WATERLOGGED, level.getFluidState(cursor).getType() == Fluids.WATER);
            if (!ladder.canSurvive(level, cursor) || !level.setBlock(cursor, ladder, Block.UPDATE_ALL)) {
                return InteractionResult.SUCCESS;
            }
            stack.consume(1, player);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, cursor);
            level.playSound(null, pos, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }
}
