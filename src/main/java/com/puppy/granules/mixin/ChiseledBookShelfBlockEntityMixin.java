package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChiseledBookShelfBlockEntity.class)
public abstract class ChiseledBookShelfBlockEntityMixin {
	@Inject(method = "acceptsItemType", at = @At("HEAD"), cancellable = true)
	private void granules$acceptEnchantedTomes(ItemStack stack, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (stack.is(GranulesMod.ENCHANTED_TOME)) {
			callbackInfo.setReturnValue(true);
		}
	}
}
