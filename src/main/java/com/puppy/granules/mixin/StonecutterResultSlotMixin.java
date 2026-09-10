package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.inventory.StonecutterMenu$2")
public abstract class StonecutterResultSlotMixin {
	@Shadow
	@Final
	private ContainerLevelAccess val$access;

	@Inject(method = "onTake", at = @At("TAIL"))
	private void granules$awardAdditionalStonecuttingResult(Player player, ItemStack stack, CallbackInfo callbackInfo) {
		if (player.level().isClientSide() || !EnchantedWorkstationAccess.isAt(val$access, GranulesMod.ENCHANTED_STONECUTTER)) {
			return;
		}
		if (player.getRandom().nextFloat() >= 0.5F) {
			return;
		}
		ItemStack additionalResult = stack.copyWithCount(1);
		if (!player.getInventory().add(additionalResult)) {
			player.drop(additionalResult, false);
		}
	}
}
