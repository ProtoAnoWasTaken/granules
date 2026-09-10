package com.puppy.granules.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LadderBlock.class)
public abstract class LadderBlockMixin {
	@Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
	private void granules$allowSupportFromAbove(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (callbackInfo.getReturnValue()) {
			return;
		}
		BlockPos abovePos = pos.above();
		BlockState aboveState = level.getBlockState(abovePos);
		if (aboveState.is(Blocks.LADDER) && aboveState.canSurvive(level, abovePos)) {
			callbackInfo.setReturnValue(true);
		}
	}

	@Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
	private void granules$removeUnsupportedLadderAfterAboveChanges(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess tickAccess,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random,
		CallbackInfoReturnable<BlockState> callbackInfo
	) {
		if (direction == Direction.UP && !state.canSurvive(level, pos)) {
			callbackInfo.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}
}
