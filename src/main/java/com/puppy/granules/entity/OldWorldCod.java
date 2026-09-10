package com.puppy.granules.entity;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.StrongholdRingLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ChunkPos;

public class OldWorldCod extends Cod {
	public OldWorldCod(EntityType<? extends Cod> type, Level level) {
		super(type, level);
	}

	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(GranulesMod.OLD_WORLD_COD_BUCKET);
	}

	public static boolean checkOldWorldCodSpawnRules(
		EntityType<OldWorldCod> type,
		ServerLevelAccessor level,
		EntitySpawnReason spawnReason,
		BlockPos pos,
		RandomSource random
	) {
		if (!WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, spawnReason, pos, random)) {
			return false;
		}
		if (!(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
		return StrongholdRingLocator.detect(serverLevel, chunkPos).isInRing();
	}
}
