package com.puppy.granules.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class BrewingStandPotionSlotMixin {
	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	private void granules$allowPotionStacks(CallbackInfoReturnable<Integer> callbackInfo) {
		callbackInfo.setReturnValue(16);
	}
}
