package com.puppy.granules.mixin;

import com.puppy.granules.access.ReverseMerchantContainerAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin implements ReverseMerchantContainerAccess {
	@Shadow
	@Final
	private Merchant merchant;

	@Shadow
	@Final
	private NonNullList<ItemStack> itemStacks;

	@Shadow
	private int selectionHint;

	@Shadow
	public abstract void setItem(int slot, ItemStack stack);

	@Unique
	private MerchantOffer granules$reverseOffer;

	@Inject(method = "updateSellItem", at = @At("TAIL"))
	private void updateReverseTradeResult(CallbackInfo callbackInfo) {
		this.granules$reverseOffer = null;
		MerchantOffers offers = this.merchant.getOffers();
		if (this.selectionHint < 0 || this.selectionHint >= offers.size()) {
			return;
		}
		ItemStack payment = this.itemStacks.get(0);
		ItemStack secondPayment = this.itemStacks.get(1);
		MerchantOffer offer = offers.get(this.selectionHint);
		ItemStack requestedStack = offer.getResult();
		if (!secondPayment.isEmpty()
			|| !ItemStack.isSameItemSameComponents(payment, requestedStack)
			|| payment.getCount() < requestedStack.getCount()) {
			return;
		}
		this.granules$reverseOffer = offer;
		this.setItem(2, offer.getCostA().copy());
		this.merchant.notifyTradeUpdated(offer.getCostA());
	}

	@Override
	public MerchantOffer granules$getReverseOffer() {
		return this.granules$reverseOffer;
	}
}
