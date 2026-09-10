package com.puppy.granules.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class EnchantedBlockItem extends BlockItem {
	public EnchantedBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
