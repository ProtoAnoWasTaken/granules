package com.puppy.granules.mixin;

import com.puppy.granules.world.SealedBarrelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntitySealedBarrelMixin {
	@Inject(method = "preRemoveSideEffects", at = @At("HEAD"), cancellable = true)
	private void granules$keepSealedContents(BlockPos pos, BlockState state, CallbackInfo callbackInfo) {
		if (state.getBlock() instanceof BarrelBlock && state.getValue(SealedBarrelProperties.SEALED)) {
			callbackInfo.cancel();
		}
	}
}
