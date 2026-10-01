package com.protoano.granules.mixin;

import com.protoano.granules.block.FenceWallOrientation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FenceBlock.class)
public abstract class FenceAxisMixin {
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

    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
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
        if (state.getValue(CrossCollisionBlock.WATERLOGGED)) {
            ticks.scheduleTick(position, net.minecraft.world.level.material.Fluids.WATER,
                net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        }
        callback.setReturnValue(granules$connections(state, level, position));
    }

    @Unique
    private static BlockState granules$connections(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos position) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction),
                FenceWallOrientation.connects(state, level, position, direction));
        }
        return state;
    }

    @Inject(method = "getOcclusionShape", at = @At("RETURN"), cancellable = true)
    private void granules$rotateOcclusion(BlockState state, CallbackInfoReturnable<net.minecraft.world.phys.shapes.VoxelShape> callback) {
        callback.setReturnValue(FenceWallOrientation.rotateShape(callback.getReturnValue(), state.getValue(FenceWallOrientation.AXIS)));
    }
}
