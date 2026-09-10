package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyloggingProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jspecify.annotations.Nullable;

@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockHoneyMixin {
	@Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
	private void granules$allowHoneyPlacement(
		@Nullable LivingEntity user,
		BlockGetter level,
		BlockPos pos,
		BlockState state,
		Fluid fluid,
		CallbackInfoReturnable<Boolean> callbackInfo
	) {
		if (fluid == GranulesMod.HONEY) {
			if (!state.hasProperty(HoneyloggingProperties.HONEYLOGGED)) {
				callbackInfo.setReturnValue(false);
				return;
			}
			callbackInfo.setReturnValue(
				!state.getValue(BlockStateProperties.WATERLOGGED) && !state.getValue(HoneyloggingProperties.HONEYLOGGED)
			);
		}
	}

	@Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
	private void granules$placeHoney(
		LevelAccessor level,
		BlockPos pos,
		BlockState state,
		FluidState fluidState,
		CallbackInfoReturnable<Boolean> callbackInfo
	) {
		if (fluidState.getType() != GranulesMod.HONEY) {
			return;
		}
		if (!state.hasProperty(HoneyloggingProperties.HONEYLOGGED)) {
			callbackInfo.setReturnValue(false);
			return;
		}
		if (!state.getValue(BlockStateProperties.WATERLOGGED) && !state.getValue(HoneyloggingProperties.HONEYLOGGED)) {
			if (!level.isClientSide()) {
				BlockState honeyloggedState = state.setValue(HoneyloggingProperties.HONEYLOGGED, true);
				level.setBlock(pos, honeyloggedState, 3);
				level.scheduleTick(pos, GranulesMod.HONEY, GranulesMod.HONEY.getTickDelay(level));
			}
			callbackInfo.setReturnValue(true);
			return;
		}
		callbackInfo.setReturnValue(false);
	}

	@Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
	private void granules$pickUpHoney(
		@Nullable LivingEntity user,
		LevelAccessor level,
		BlockPos pos,
		BlockState state,
		CallbackInfoReturnable<ItemStack> callbackInfo
	) {
		if (!state.hasProperty(HoneyloggingProperties.HONEYLOGGED) || !state.getValue(HoneyloggingProperties.HONEYLOGGED)) {
			return;
		}
		level.setBlock(pos, state.setValue(HoneyloggingProperties.HONEYLOGGED, false), 3);
		if (!state.canSurvive(level, pos)) {
			level.destroyBlock(pos, true);
		}
		callbackInfo.setReturnValue(new ItemStack(GranulesMod.HONEY_BUCKET));
	}
}
