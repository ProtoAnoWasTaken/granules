package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.mixin.access.ItemCombinerMenuAccess;
import com.puppy.granules.world.EnchantedSmithingMenuAccess;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin implements EnchantedSmithingMenuAccess {
	@Unique
	private ItemStack granules$retainedTemplate = ItemStack.EMPTY;

	@Unique
	private boolean granules$usedEnchantedSmithingTable;

	@Inject(method = "isValidBlock", at = @At("RETURN"), cancellable = true)
	private void granules$acceptEnchantedSmithingTable(BlockState state, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (state.is(GranulesMod.ENCHANTED_SMITHING_TABLE)) {
			callbackInfo.setReturnValue(true);
		}
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void granules$selectTemplateToRetain(Player player, ItemStack result, CallbackInfo callbackInfo) {
		if (player.level().isClientSide() || !granules$isEnchantedSmithingTable()) {
			return;
		}
		granules$usedEnchantedSmithingTable = true;
		Container inputSlots = ((ItemCombinerMenuAccess) this).granules$getInputSlots();
		ItemStack template = inputSlots.getItem(SmithingMenu.TEMPLATE_SLOT);
		if (template.getItem() instanceof SmithingTemplateItem && player.getRandom().nextFloat() < 0.25F) {
			granules$retainedTemplate = template.copyWithCount(1);
		}
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	private void granules$rejectTomesAtEnchantedSmithingTable(CallbackInfo callbackInfo) {
		if (!granules$isEnchantedSmithingTable()) {
			return;
		}
		ItemCombinerMenuAccess menu = (ItemCombinerMenuAccess) this;
		if (menu.granules$getInputSlots().getItem(SmithingMenu.TEMPLATE_SLOT).is(GranulesMod.ENCHANTED_TOME)) {
			menu.granules$getResultSlots().setItem(0, ItemStack.EMPTY);
		}
	}

	@Inject(method = "onTake", at = @At("TAIL"))
	private void granules$returnRetainedTemplate(Player player, ItemStack result, CallbackInfo callbackInfo) {
		if (player.level().isClientSide() || granules$retainedTemplate.isEmpty()) {
			granules$retainedTemplate = ItemStack.EMPTY;
			return;
		}
		Container inputSlots = ((ItemCombinerMenuAccess) this).granules$getInputSlots();
		ItemStack currentTemplate = inputSlots.getItem(SmithingMenu.TEMPLATE_SLOT);
		if (currentTemplate.isEmpty()) {
			inputSlots.setItem(SmithingMenu.TEMPLATE_SLOT, granules$retainedTemplate);
		} else if (ItemStack.isSameItemSameComponents(currentTemplate, granules$retainedTemplate) && currentTemplate.getCount() < currentTemplate.getMaxStackSize()) {
			currentTemplate.grow(1);
			inputSlots.setItem(SmithingMenu.TEMPLATE_SLOT, currentTemplate);
		} else if (!player.getInventory().add(granules$retainedTemplate)) {
			player.drop(granules$retainedTemplate, false);
		}
		granules$retainedTemplate = ItemStack.EMPTY;
	}

	@Override
	public boolean granules$consumeUsedEnchantedSmithingTable() {
		boolean usedEnchantedSmithingTable = granules$usedEnchantedSmithingTable;
		granules$usedEnchantedSmithingTable = false;
		return usedEnchantedSmithingTable;
	}

	@Unique
	private boolean granules$isEnchantedSmithingTable() {
		return EnchantedWorkstationAccess.isAt(
			((ItemCombinerMenuAccess) this).granules$getAccess(),
			GranulesMod.ENCHANTED_SMITHING_TABLE
		);
	}
}
