package com.puppy.granules.mixin;

import com.puppy.granules.world.EnchantedCraftingMenuAccess;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ResultSlot.class)
public abstract class EnchantedCraftingResultSlotMixin {
	@Shadow
	@Final
	private CraftingContainer craftSlots;

	@Unique
	private List<RetainedIngredient> granules$retainedIngredients = List.of();

	@Inject(method = "onTake", at = @At("HEAD"))
	private void granules$selectRetainedIngredients(Player player, ItemStack stack, CallbackInfo callbackInfo) {
		if (!granules$isEnchantedCraftingTable(player) || player.level().isClientSide()) {
			return;
		}
		List<RetainedIngredient> retainedIngredients = new ArrayList<>();
		for (int slot = 0; slot < craftSlots.getContainerSize(); slot++) {
			ItemStack ingredient = craftSlots.getItem(slot);
			if (!ingredient.isEmpty() && player.getRandom().nextFloat() < 0.1F) {
				retainedIngredients.add(new RetainedIngredient(slot, ingredient.copyWithCount(1)));
			}
		}
		granules$retainedIngredients = retainedIngredients;
	}

	@Inject(method = "onTake", at = @At("TAIL"))
	private void granules$returnRetainedIngredients(Player player, ItemStack stack, CallbackInfo callbackInfo) {
		if (!granules$isEnchantedCraftingTable(player) || player.level().isClientSide()) {
			granules$retainedIngredients = List.of();
			return;
		}
		for (RetainedIngredient retainedIngredient : granules$retainedIngredients) {
			granules$returnToCraftingSlot(player, retainedIngredient);
		}
		player.addEffect(new MobEffectInstance(MobEffects.HASTE, 2400, 0));
		granules$retainedIngredients = List.of();
	}

	@Unique
	private boolean granules$isEnchantedCraftingTable(Player player) {
		if (!(player.containerMenu instanceof CraftingMenu craftingMenu)) {
			return false;
		}
		return ((EnchantedCraftingMenuAccess) craftingMenu).granules$isEnchantedCraftingTable();
	}

	@Unique
	private void granules$returnToCraftingSlot(Player player, RetainedIngredient retainedIngredient) {
		ItemStack currentItem = craftSlots.getItem(retainedIngredient.slot());
		ItemStack retainedItem = retainedIngredient.item();
		if (currentItem.isEmpty()) {
			craftSlots.setItem(retainedIngredient.slot(), retainedItem);
			return;
		}
		if (ItemStack.isSameItemSameComponents(currentItem, retainedItem) && currentItem.getCount() < currentItem.getMaxStackSize()) {
			currentItem.grow(1);
			craftSlots.setItem(retainedIngredient.slot(), currentItem);
			return;
		}
		if (!player.getInventory().add(retainedItem)) {
			player.drop(retainedItem, false);
		}
	}

	@Unique
	private record RetainedIngredient(int slot, ItemStack item) {
	}
}
