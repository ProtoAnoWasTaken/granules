package com.puppy.granules.mixin;

import com.puppy.granules.fletching.ArrowParts;
import com.puppy.granules.fletching.FletchersArrowItem;
import com.puppy.granules.rabbit.RabbitContent;
import com.puppy.granules.config.ContentManifest;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerTradeMixin {
	private static final List<ArrowParts> FLETCHERS_ARROW_TRADE_PARTS = ArrowParts.ALL.stream()
		.filter(parts -> !parts.isBasic())
		.toList();

	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void replaceExpertFeatherTrade(ServerLevel level, CallbackInfo callbackInfo) {
		Villager villager = (Villager) (Object) this;
		if (!ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
			addPalePeltTrade(villager);
		}
		if (ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)
			|| !villager.getVillagerData().profession().is(VillagerProfession.FLETCHER)
			|| villager.getVillagerData().level() != 4
			|| !villager.getRandom().nextBoolean()) {
			return;
		}
		MerchantOffers offers = villager.getOffers();
		for (int index = 0; index < offers.size(); index++) {
			MerchantOffer offer = offers.get(index);
			if (!isExpertFeatherTrade(offer)) {
				continue;
			}
			ArrowParts parts = FLETCHERS_ARROW_TRADE_PARTS.get(villager.getRandom().nextInt(FLETCHERS_ARROW_TRADE_PARTS.size()));
			offers.set(index, new MerchantOffer(
				new ItemCost(Items.EMERALD, 3),
				FletchersArrowItem.createStack(parts, 8),
				3,
				30,
				0.05F
			));
			return;
		}
	}

	private void addPalePeltTrade(Villager villager) {
		if (!villager.getVillagerData().profession().is(VillagerProfession.LEATHERWORKER)
			|| villager.getVillagerData().level() != 3) {
			return;
		}
		MerchantOffers offers = villager.getOffers();
		boolean rabbitHideTrade = offers.stream().anyMatch(offer -> {
			ItemStack cost = offer.getBaseCostA();
			return cost.is(Items.RABBIT_HIDE) && cost.getCount() == 9 && offer.getResult().is(Items.EMERALD);
		});
		if (rabbitHideTrade) {
			offers.add(new MerchantOffer(new ItemCost(RabbitContent.PALE_PELT, 9), new ItemStack(Items.EMERALD), 12, 20, 0.05F));
		}
	}

	private boolean isExpertFeatherTrade(MerchantOffer offer) {
		ItemStack cost = offer.getBaseCostA();
		return cost.is(Items.FEATHER)
			&& cost.getCount() == 24
			&& offer.getResult().is(Items.EMERALD);
	}
}
