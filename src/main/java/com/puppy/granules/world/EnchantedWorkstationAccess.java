package com.puppy.granules.world;

import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;

public final class EnchantedWorkstationAccess {
	private EnchantedWorkstationAccess() {
	}

	public static boolean isAt(ContainerLevelAccess access, Block block) {
		boolean[] result = {false};
		access.execute((level, pos) -> result[0] = level.getBlockState(pos).is(block));
		return result[0];
	}
}
