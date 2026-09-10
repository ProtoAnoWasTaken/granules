package com.puppy.granules.access;

import net.minecraft.core.BlockPos;

public interface ServerPlayerGameModeAccess {
	boolean granules$isDestroyingBlock();

	BlockPos granules$getDestroyPos();
}
