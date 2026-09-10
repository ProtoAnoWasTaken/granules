package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public final class EnchantedEnchantingTableRecovery {
	private EnchantedEnchantingTableRecovery() {
	}

	public static void initialize() {
		ServerChunkEvents.CHUNK_LOAD.register(EnchantedEnchantingTableRecovery::restoreMissingBookEntities);
	}

	private static void restoreMissingBookEntities(ServerLevel level, LevelChunk chunk, boolean newlyGenerated) {
		if (newlyGenerated) {
			return;
		}
		int minimumBuildHeight = level.getMinY();
		int maximumBuildHeight = level.getMaxY();
		for (int y = minimumBuildHeight; y < maximumBuildHeight; y++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				for (int localX = 0; localX < 16; localX++) {
					BlockPos pos = new BlockPos(chunk.getPos().getBlockX(localX), y, chunk.getPos().getBlockZ(localZ));
					if (chunk.getBlockState(pos).is(GranulesMod.ENCHANTED_ENCHANTING_TABLE) && chunk.getBlockEntity(pos) == null) {
						chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
					}
				}
			}
		}
	}
}
