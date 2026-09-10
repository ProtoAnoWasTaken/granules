package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public class BlockEntityEnchantedTableMixin {
	@Inject(method = "validateBlockState", at = @At("HEAD"), cancellable = true)
	private void allowEnchantedTableBlockState(BlockState state, CallbackInfo callbackInfo) {
		if (state.is(GranulesMod.ENCHANTED_ENCHANTING_TABLE)) {
			callbackInfo.cancel();
		}
	}

	@Inject(method = "isValidBlockState", at = @At("HEAD"), cancellable = true)
	private void allowEnchantedTableTicking(BlockState state, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (state.is(GranulesMod.ENCHANTED_ENCHANTING_TABLE)) {
			callbackInfo.setReturnValue(true);
		}
	}
}
