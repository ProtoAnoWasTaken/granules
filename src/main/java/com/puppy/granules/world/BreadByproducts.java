package com.puppy.granules.world;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BreadByproducts {
	private BreadByproducts() {
	}

	public static boolean shouldCreateBreadHeels(Player player) {
		return rollWithLuck(player, 25, 10, 5);
	}

	public static boolean shouldCreateBreadCrumbs(Player player) {
		return rollWithLuck(player, 5, 3, 2);
	}

	public static void give(Player player, Item item) {
		ItemStack stack = new ItemStack(item);
		if (!player.addItem(stack)) {
			player.drop(stack, false);
		}
	}

	private static boolean rollWithLuck(Player player, int withoutLuckDenominator, int luckOneDenominator, int luckTwoDenominator) {
		MobEffectInstance luck = player.getEffect(MobEffects.LUCK);
		if (luck == null) {
			return player.getRandom().nextInt(withoutLuckDenominator) == 0;
		}
		int denominator = luck.getAmplifier() >= 1 ? luckTwoDenominator : luckOneDenominator;
		return player.getRandom().nextInt(denominator) == 0;
	}
}
