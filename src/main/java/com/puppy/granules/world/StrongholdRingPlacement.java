package com.puppy.granules.world;

import com.mojang.serialization.MapCodec;
import com.puppy.granules.GranulesMod;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.util.RandomSource;

public final class StrongholdRingPlacement extends PlacementModifier {
	public static final StrongholdRingPlacement INSTANCE = new StrongholdRingPlacement();
	public static final MapCodec<StrongholdRingPlacement> CODEC = MapCodec.unit(INSTANCE);

	private StrongholdRingPlacement() {
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos origin) {
		ChunkPos chunkPos = new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
		if (StrongholdRingLocator.detect(context.getLevel().getLevel(), chunkPos).isInRing()) {
			return Stream.of(origin);
		}
		return Stream.empty();
	}

	@Override
	public PlacementModifierType<?> type() {
		return GranulesMod.STRONGHOLD_RING_PLACEMENT;
	}
}
