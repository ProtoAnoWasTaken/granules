package com.puppy.granules.block;

import com.puppy.granules.GranulesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantedWorkstationBlockEntity extends BlockEntity {
	public EnchantedWorkstationBlockEntity(BlockPos pos, BlockState state) {
		super(GranulesMod.ENCHANTED_WORKSTATION_BLOCK_ENTITY, pos, state);
	}
}
