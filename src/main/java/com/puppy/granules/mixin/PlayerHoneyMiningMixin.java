package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerHoneyMiningMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void granules$slowMiningInHoney(BlockState state, CallbackInfoReturnable<Float> callbackInfo) {
		Player player = (Player) (Object) this;
		if (player.getFluidHeight(GranulesMod.HONEY_TAG) <= 0.0D || granules$hasAquaAffinity(player)) {
			return;
		}
		callbackInfo.setReturnValue(callbackInfo.getReturnValue() * 0.8F);
	}

	private boolean granules$hasAquaAffinity(Player player) {
		Holder<Enchantment> aquaAffinity = player.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.AQUA_AFFINITY);
		return EnchantmentHelper.getEnchantmentLevel(aquaAffinity, player) > 0;
	}
}
