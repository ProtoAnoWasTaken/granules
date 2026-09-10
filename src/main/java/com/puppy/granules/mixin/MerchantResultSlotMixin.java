package com.puppy.granules.mixin;

import com.puppy.granules.access.ReverseMerchantContainerAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
	@Shadow
	@Final
	private MerchantContainer slots;

	@Shadow
	@Final
	private Merchant merchant;

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void completeReverseTrade(Player player, ItemStack stack, CallbackInfo callbackInfo) {
		MerchantOffer offer = ((ReverseMerchantContainerAccess) this.slots).granules$getReverseOffer();
		if (offer == null) {
			return;
		}
		ItemStack payment = this.slots.getItem(0);
		ItemStack requestedStack = offer.getResult();
		if (!ItemStack.isSameItemSameComponents(payment, requestedStack) || payment.getCount() < requestedStack.getCount()) {
			return;
		}
		payment.shrink(requestedStack.getCount());
		this.slots.setItem(0, payment);
		((MerchantOfferAccessor) offer).granules$setUses(Math.max(0, offer.getUses() - 1));
		if (!player.level().isClientSide()) {
			this.merchant.notifyTradeUpdated(this.slots.getItem(2));
			ItemStack secondCost = offer.getCostB();
			if (!secondCost.isEmpty()) {
				ItemStack copy = secondCost.copy();
				if (!player.getInventory().add(copy)) {
					player.drop(copy, false);
				}
			}
		}
		callbackInfo.cancel();
	}
}
