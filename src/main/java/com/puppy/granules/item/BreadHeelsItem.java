package com.puppy.granules.item;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.BreadByproducts;
import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BreadHeelsItem extends Item {
	public BreadHeelsItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(itemStack, level, entity);
		if (!level.isClientSide() && entity instanceof Player player && BreadByproducts.shouldCreateBreadCrumbs(player)) {
			BreadByproducts.give(player, GranulesMod.BREAD_CRUMBS);
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				GranulesAdvancements.award(serverPlayer, "crummy_deal");
			}
		}
		return result;
	}
}
