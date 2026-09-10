package com.puppy.granules.mixin;

import com.puppy.granules.world.EnchantedSmithingMenuAccess;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuEffectMixin {
	@Inject(method = "removed", at = @At("TAIL"))
	private void granules$grantResistanceAfterSmithing(Player player, CallbackInfo callbackInfo) {
		if (player.level().isClientSide() || !((Object) this instanceof SmithingMenu smithingMenu)) {
			return;
		}
		EnchantedSmithingMenuAccess access = (EnchantedSmithingMenuAccess) smithingMenu;
		if (access.granules$consumeUsedEnchantedSmithingTable()) {
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2400, 0));
		}
	}
}
