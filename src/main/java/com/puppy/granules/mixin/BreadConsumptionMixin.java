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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class BreadConsumptionMixin {
	@Inject(method = "finishUsingItem", at = @At("RETURN"))
	private void granules$createBreadHeels(ItemStack itemStack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callbackInfo) {
		if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)
			&& !level.isClientSide()
			&& itemStack.is(Items.BREAD)
			&& entity instanceof Player player
			&& BreadByproducts.shouldCreateBreadHeels(player)) {
			BreadByproducts.give(player, GranulesMod.BREAD_HEELS);
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				GranulesAdvancements.award(serverPlayer, "1080_heelflip");
			}
		}
	}
}
