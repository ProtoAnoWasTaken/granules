package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
	private void granules$remainValidAtEnchantedStonecutter(Player player, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (EnchantedWorkstationAccess.isAt(access, GranulesMod.ENCHANTED_STONECUTTER)) {
			callbackInfo.setReturnValue(true);
		}
	}
}
