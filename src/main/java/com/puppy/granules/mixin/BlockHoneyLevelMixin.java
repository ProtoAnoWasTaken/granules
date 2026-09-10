package com.puppy.granules.mixin;

import com.puppy.granules.world.HoneyCauldrons;
import com.puppy.granules.block.EnchantedWorkstationBlocks;
import com.puppy.granules.world.EnchantedCauldronProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockHoneyLevelMixin {
	@Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
	private void granules$addHoneyLevel(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo callbackInfo) {
		if ((Object) this instanceof CauldronBlock) {
			builder.add(HoneyCauldrons.HONEY_LEVEL);
		}
		if ((Object) this instanceof EnchantedWorkstationBlocks.EnchantedCauldron) {
			builder.add(EnchantedCauldronProperties.CONTENT, EnchantedCauldronProperties.LEVEL);
		}
	}
}
