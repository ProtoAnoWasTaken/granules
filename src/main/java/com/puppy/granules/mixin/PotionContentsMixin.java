package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionContents.class)
public abstract class PotionContentsMixin {
	@Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
	private void granules$useWheatMasterColor(CallbackInfoReturnable<Integer> callbackInfo) {
		PotionContents contents = (PotionContents) (Object) this;
		if (
			contents.is(GranulesMod.WHEAT_MASTER)
				|| contents.is(GranulesMod.LONG_WHEAT_MASTER)
				|| contents.is(GranulesMod.STRONG_WHEAT_MASTER)
		) {
			callbackInfo.setReturnValue(0xE6B749);
		}
	}
}
