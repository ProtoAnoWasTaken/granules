package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.mixin.access.AbstractContainerMenuAccess;
import com.puppy.granules.mixin.access.ItemCombinerMenuAccess;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private String itemName;

	@Unique
	private final DataSlot granules$enchantedAnvil = DataSlot.standalone();

	@Inject(
		method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
		at = @At("TAIL")
	)
	private void granules$syncEnchantedAnvil(
		int containerId,
		Inventory inventory,
		ContainerLevelAccess access,
		CallbackInfo callbackInfo
	) {
		granules$enchantedAnvil.set(EnchantedWorkstationAccess.isAt(access, GranulesMod.ENCHANTED_ANVIL) ? 1 : 0);
		((AbstractContainerMenuAccess) (Object) this).granules$addDataSlot(granules$enchantedAnvil);
	}

	@Inject(method = "isValidBlock", at = @At("RETURN"), cancellable = true)
	private void granules$acceptEnchantedAnvil(BlockState state, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (state.is(GranulesMod.ENCHANTED_ANVIL)) {
			callbackInfo.setReturnValue(true);
		}
	}

	@Redirect(
		method = "createResult",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"
		)
	)
	private Object granules$ignorePriorWorkCosts(
		ItemStack stack,
		DataComponentType<Object> componentType,
		Object fallback
	) {
		if (componentType.equals(DataComponents.REPAIR_COST) && granules$isEnchantedAnvil()) {
			return 0;
		}

		return stack.getOrDefault(componentType, fallback);
	}

	@Inject(method = "createResult", at = @At("RETURN"))
	private void granules$createRenameOnlyResultAtEnchantedAnvil(CallbackInfo callbackInfo) {
		if (!granules$isEnchantedAnvil() || itemName == null) {
			return;
		}
		ItemCombinerMenuAccess menu = (ItemCombinerMenuAccess) this;
		ItemStack input = menu.granules$getInputSlots().getItem(0);
		ItemStack additionalInput = menu.granules$getInputSlots().getItem(1);
		if (input.isEmpty() || !additionalInput.isEmpty()) {
			return;
		}
		boolean clearingName = StringUtil.isBlank(itemName);
		boolean changesName = clearingName
			? input.has(DataComponents.CUSTOM_NAME)
			: !itemName.equals(input.getHoverName().getString());
		if (!changesName) {
			menu.granules$getResultSlots().setItem(0, ItemStack.EMPTY);
			cost.set(0);
			return;
		}
		ItemStack renamed = input.copy();
		if (clearingName) {
			renamed.remove(DataComponents.CUSTOM_NAME);
		} else {
			renamed.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
		}
		renamed.remove(DataComponents.REPAIR_COST);
		menu.granules$getResultSlots().setItem(0, renamed);
		cost.set(1);
	}

	@Inject(method = "createResult", at = @At("RETURN"))
	private void granules$clearResultPriorWorkCost(CallbackInfo callbackInfo) {
		if (!granules$isEnchantedAnvil()) {
			return;
		}

		ItemStack result = ((ItemCombinerMenuAccess) this).granules$getResultSlots().getItem(0);
		if (!result.isEmpty() && result.has(DataComponents.REPAIR_COST)) {
			result.remove(DataComponents.REPAIR_COST);
			((ItemCombinerMenuAccess) this).granules$getResultSlots().setItem(0, result);
		}
	}

	@Unique
	private boolean granules$isEnchantedAnvil() {
		if (granules$enchantedAnvil.get() == 1) {
			return true;
		}

		return EnchantedWorkstationAccess.isAt(
			((ItemCombinerMenuAccess) this).granules$getAccess(),
			GranulesMod.ENCHANTED_ANVIL
		);
	}

	@Redirect(
		method = "lambda$onTake$0",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AnvilBlock;damage(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;")
	)
	private static BlockState granules$preserveEnchantedAnvil(BlockState state) {
		if (state.is(GranulesMod.ENCHANTED_ANVIL)) {
			return state;
		}
		return AnvilBlock.damage(state);
	}
}
