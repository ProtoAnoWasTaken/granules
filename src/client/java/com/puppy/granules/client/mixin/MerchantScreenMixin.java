package com.puppy.granules.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {
	private static final Identifier REVERSE_TRADE_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/villager/poly");
	private static final Identifier REVERSE_TRADE_UNAVAILABLE_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/villager/out_of_stock_poly");
	private static final Identifier REVERSE_TRADE_ARROW_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/villager/trade_arrow_poly");
	private static final Identifier REVERSE_TRADE_ARROW_UNAVAILABLE_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/villager/trade_arrow_poly_out_of_stock");

	@Shadow
	private int shopItem;

	@Shadow
	private int scrollOff;

	@Inject(method = "extractContents", at = @At("TAIL"))
	private void renderReverseTradeControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo callbackInfo) {
		MerchantOffers offers = menu().getOffers();
		if (offers.isEmpty() || this.shopItem < 0 || this.shopItem >= offers.size()) {
			return;
		}
		MerchantOffer selectedOffer = offers.get(this.shopItem);
		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED,
			selectedOffer.isOutOfStock() ? REVERSE_TRADE_UNAVAILABLE_SPRITE : REVERSE_TRADE_SPRITE,
			leftPos() + 182,
			topPos() + 35,
			28,
			21
		);
		int firstVisibleOffer = this.scrollOff;
		int lastVisibleOffer = Math.min(offers.size(), this.scrollOff + 7);
		for (int offerIndex = firstVisibleOffer; offerIndex < lastVisibleOffer; offerIndex++) {
			MerchantOffer offer = offers.get(offerIndex);
			int row = offerIndex - this.scrollOff;
			graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				offer.isOutOfStock() ? REVERSE_TRADE_ARROW_UNAVAILABLE_SPRITE : REVERSE_TRADE_ARROW_SPRITE,
				leftPos() + 53,
				topPos() + 22 + row * 20,
				17,
				9
			);
		}
	}

	private MerchantMenu menu() {
		return ((MerchantScreen) (Object) this).getMenu();
	}

	private int leftPos() {
		return ((AbstractContainerScreenAccessor) this).granules$getLeftPos();
	}

	private int topPos() {
		return ((AbstractContainerScreenAccessor) this).granules$getTopPos();
	}
}
