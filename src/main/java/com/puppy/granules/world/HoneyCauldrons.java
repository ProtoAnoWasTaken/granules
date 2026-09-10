package com.puppy.granules.world;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class HoneyCauldrons {
	public static final IntegerProperty HONEY_LEVEL = IntegerProperty.create("honey_level", 0, 3);

	private HoneyCauldrons() {
	}

	public static int getHoneyLevel(BlockState state) {
		return state.getValue(HONEY_LEVEL);
	}

	public static boolean hasHoney(BlockState state) {
		return getHoneyLevel(state) > 0;
	}

	public static boolean isFull(BlockState state) {
		return getHoneyLevel(state) == 3;
	}

	public static BlockState withHoneyLevel(BlockState state, int level) {
		return state.setValue(HONEY_LEVEL, level);
	}
}
