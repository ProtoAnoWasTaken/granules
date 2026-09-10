package com.puppy.granules.client;

import com.puppy.granules.world.EnchantedCauldronProperties;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantedCauldronTintSource implements BlockTintSource {
	@Override
	public int color(BlockState state) {
		return -1;
	}

	@Override
	public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		if (state.getValue(EnchantedCauldronProperties.CONTENT) == EnchantedCauldronProperties.Content.WATER) {
			return BiomeColors.getAverageWaterColor(level, pos);
		}
		return -1;
	}
}
