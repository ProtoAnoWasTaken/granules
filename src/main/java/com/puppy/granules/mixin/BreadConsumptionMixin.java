package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.BreadByproducts;
import com.puppy.granules.config.ContentManifest;
import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Item.class)
public abstract class BreadConsumptionMixin {
	@WrapMethod(method = "finishUsingItem")
	private ItemStack granules$createBreadHeels(ItemStack itemStack, Level level, LivingEntity entity, Operation<ItemStack> original) {
		boolean bread = itemStack.is(Items.BREAD);
		ItemStack result = original.call(itemStack, level, entity);
		if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)
			&& !level.isClientSide()
			&& bread
			&& entity instanceof Player player
			&& BreadByproducts.shouldCreateBreadHeels(player)) {
			BreadByproducts.give(player, GranulesMod.BREAD_HEELS);
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				GranulesAdvancements.award(serverPlayer, "1080_heelflip");
			}
		}
		return result;
	}
}
