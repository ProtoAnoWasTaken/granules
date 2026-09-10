package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Shadow
	@Final
	private Container resultSlots;

	@Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
	private void granules$remainValidAtEnchantedGrindstone(Player player, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (EnchantedWorkstationAccess.isAt(access, GranulesMod.ENCHANTED_GRINDSTONE)) {
			callbackInfo.setReturnValue(true);
		}
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void granules$removeCurses(CallbackInfo callbackInfo) {
		if (!EnchantedWorkstationAccess.isAt(access, GranulesMod.ENCHANTED_GRINDSTONE)) {
			return;
		}
		ItemStack result = resultSlots.getItem(0);
		if (!result.isEmpty()) {
			EnchantmentHelper.setEnchantments(result, ItemEnchantments.EMPTY);
		}
	}
}
