package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeEnchantedTableMixin {
	@Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
	private void allowEnchantedTableBlockState(BlockState state, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (state.is(GranulesMod.ENCHANTED_ENCHANTING_TABLE)) {
			callbackInfo.setReturnValue(true);
		}
	}
}
