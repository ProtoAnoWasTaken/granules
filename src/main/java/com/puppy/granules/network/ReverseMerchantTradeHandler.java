package com.puppy.granules.network;

import com.puppy.granules.mixin.MerchantOfferAccessor;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class ReverseMerchantTradeHandler {
	private ReverseMerchantTradeHandler() {
	}

	public static void trade(ServerPlayer player, int offerIndex) {
		if (!(player.containerMenu instanceof MerchantMenu menu) || !menu.stillValid(player)) {
			return;
		}
		MerchantOffers offers = menu.getOffers();
		if (offerIndex < 0 || offerIndex >= offers.size()) {
			return;
		}
		MerchantOffer offer = offers.get(offerIndex);
		ItemStack requestedStack = offer.getResult();
		if (countMatchingItems(player.getInventory(), requestedStack) < requestedStack.getCount()) {
			return;
		}
		removeMatchingItems(player.getInventory(), requestedStack, requestedStack.getCount());
		giveItem(player, offer.getCostA());
		giveItem(player, offer.getCostB());
		MerchantOfferAccessor accessor = (MerchantOfferAccessor) offer;
		accessor.granules$setUses(Math.max(0, offer.getUses() - 1));
		menu.broadcastChanges();
		player.connection.send(new ClientboundMerchantOffersPacket(
			menu.containerId,
			offers,
			menu.getTraderLevel(),
			menu.getTraderXp(),
			menu.showProgressBar(),
			menu.canRestock()
		));
	}

	private static int countMatchingItems(Inventory inventory, ItemStack requestedStack) {
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (ItemStack.isSameItemSameComponents(stack, requestedStack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static void removeMatchingItems(Inventory inventory, ItemStack requestedStack, int count) {
		int remaining = count;
		for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!ItemStack.isSameItemSameComponents(stack, requestedStack)) {
				continue;
			}
			int removed = Math.min(remaining, stack.getCount());
			stack.shrink(removed);
			remaining -= removed;
		}
		inventory.setChanged();
	}

	private static void giveItem(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		ItemStack copy = stack.copy();
		if (!player.getInventory().add(copy)) {
			player.drop(copy, false);
		}
	}
}
