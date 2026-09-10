package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

public final class OldWorldRoseBonemeal {
	private OldWorldRoseBonemeal() {
	}

	public static void tryGrow(ServerLevel level, RandomSource random, BlockPos origin) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS) || random.nextInt(8) != 0) {
			return;
		}
		for (int attempt = 0; attempt < 8; attempt++) {
			BlockPos target = origin.above().offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
			ChunkPos chunkPos = new ChunkPos(target.getX() >> 4, target.getZ() >> 4);
			if (!StrongholdRingLocator.detect(level, chunkPos).isInRing()) {
				continue;
			}
			BlockState flower = random.nextInt(25) == 0
				? GranulesMod.OLD_WORLD_CYAN_ROSE.defaultBlockState()
				: GranulesMod.OLD_WORLD_ROSE.defaultBlockState();
			if (level.isEmptyBlock(target) && flower.canSurvive(level, target)) {
				level.setBlock(target, flower, 3);
				return;
			}
		}
	}
}
