package com.protoano.granules.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandFeatherFallingMixin {
	@Redirect(
		method = "fallOn",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V")
	)
	private static void preserveFarmlandForFeatherFalling(Entity entity, BlockState state, Level level, BlockPos pos) {
		if (entity instanceof Player player && hasFeatherFalling(player, level)) {
			return;
		}
		FarmlandBlock.turnToDirt(entity, state, level, pos);
	}

	private static boolean hasFeatherFalling(Player player, Level level) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		Holder<Enchantment> enchantment = serverLevel.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(Enchantments.FEATHER_FALLING);
		return EnchantmentHelper.getEnchantmentLevel(enchantment, player) > 0;
	}
}
