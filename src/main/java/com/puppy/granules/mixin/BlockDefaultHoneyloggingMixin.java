package com.puppy.granules.mixin;

import com.puppy.granules.world.HoneyloggingProperties;
import com.puppy.granules.world.EnchantedCauldronProperties;
import com.puppy.granules.world.SealedBarrelProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Block.class)
public abstract class BlockDefaultHoneyloggingMixin {
	@ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
	private BlockState granules$defaultToDryHoneylogging(BlockState state) {
		if (state.hasProperty(HoneyloggingProperties.HONEYLOGGED)) {
			state = state.setValue(HoneyloggingProperties.HONEYLOGGED, false);
		}
		if (state.hasProperty(EnchantedCauldronProperties.CONTENT)) {
			state = state.setValue(EnchantedCauldronProperties.CONTENT, EnchantedCauldronProperties.Content.EMPTY);
		}
		if (state.hasProperty(EnchantedCauldronProperties.LEVEL)) {
			state = state.setValue(EnchantedCauldronProperties.LEVEL, 0);
		}
		if (state.hasProperty(SealedBarrelProperties.SEALED)) {
			state = state.setValue(SealedBarrelProperties.SEALED, false);
		}
		return state;
	}
}
