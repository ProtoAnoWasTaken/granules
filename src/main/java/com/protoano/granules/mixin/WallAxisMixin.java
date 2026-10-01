package com.protoano.granules.mixin;

import com.protoano.granules.block.FenceWallOrientation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WallBlock.class)
public abstract class WallAxisMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract BlockState updateSides(BlockState state, boolean north, boolean east, boolean south, boolean west,
        net.minecraft.world.phys.shapes.VoxelShape aboveShape);

    @org.spongepowered.asm.mixin.Shadow
    protected abstract boolean shouldRaisePost(BlockState state, BlockState aboveState, net.minecraft.world.phys.shapes.VoxelShape aboveShape);

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void granules$addAxis(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder, CallbackInfo callback) {
        builder.add(FenceWallOrientation.AXIS);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void granules$setAxis(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callback) {
        BlockState state = callback.getReturnValue();
        if (state == null) {
            return;
        }
        state = state.setValue(FenceWallOrientation.AXIS, context.getClickedFace().getAxis());
        callback.setReturnValue(granules$connections(state, context.getLevel(), context.getClickedPos()));
    }

    @Inject(method = "updateShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true)
    private void granules$limitUpdates(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        net.minecraft.core.BlockPos position,
        Direction direction,
        net.minecraft.core.BlockPos neighborPosition,
        BlockState neighborState,
        RandomSource random,
        CallbackInfoReturnable<BlockState> callback
    ) {
        if (state.getValue(WallBlock.WATERLOGGED)) {
            ticks.scheduleTick(position, net.minecraft.world.level.material.Fluids.WATER,
                net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        }
        callback.setReturnValue(granules$connections(state, level, position));
    }

    @Unique
    private BlockState granules$connections(BlockState state, LevelReader level, net.minecraft.core.BlockPos position) {
        Direction.Axis axis = state.getValue(FenceWallOrientation.AXIS);
        net.minecraft.core.BlockPos abovePosition = position.relative(FenceWallOrientation.toWorld(Direction.UP, axis));
        BlockState aboveState = level.getBlockState(abovePosition);
        if (aboveState.hasProperty(FenceWallOrientation.AXIS) && !FenceWallOrientation.matches(aboveState, axis)) {
            aboveState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        net.minecraft.world.phys.shapes.VoxelShape aboveShape = FenceWallOrientation.unrotateShape(
            aboveState.getCollisionShape(level, abovePosition), axis).getFaceShape(Direction.DOWN);
        state = updateSides(state,
            FenceWallOrientation.connects(state, level, position, Direction.NORTH),
            FenceWallOrientation.connects(state, level, position, Direction.EAST),
            FenceWallOrientation.connects(state, level, position, Direction.SOUTH),
            FenceWallOrientation.connects(state, level, position, Direction.WEST), aboveShape);
        return state.setValue(WallBlock.UP, shouldRaisePost(state, aboveState, aboveShape));
    }

    @Inject(method = {"getShape", "getCollisionShape"}, at = @At("RETURN"), cancellable = true)
    private void granules$rotateShape(BlockState state, net.minecraft.world.level.BlockGetter level,
        net.minecraft.core.BlockPos position, net.minecraft.world.phys.shapes.CollisionContext context,
        CallbackInfoReturnable<net.minecraft.world.phys.shapes.VoxelShape> callback) {
        callback.setReturnValue(FenceWallOrientation.rotateShape(callback.getReturnValue(), state.getValue(FenceWallOrientation.AXIS)));
    }
}
