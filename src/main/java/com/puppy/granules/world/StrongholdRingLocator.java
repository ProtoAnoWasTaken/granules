package com.puppy.granules.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

public final class StrongholdRingLocator {
	private static final ResourceKey<StructureSet> STRONGHOLD_STRUCTURE_SET = ResourceKey.create(
		Registries.STRUCTURE_SET,
		Identifier.withDefaultNamespace("strongholds")
	);
	private static final Map<ServerLevel, RingData> RING_DATA = new WeakHashMap<>();

	private StrongholdRingLocator() {
	}

	public static Detection detect(ServerLevel level, ChunkPos chunkPos) {
		RingData ringData = RING_DATA.computeIfAbsent(level, StrongholdRingLocator::calculateRingData);
		return ringData.detect(chunkPos);
	}

	private static RingData calculateRingData(ServerLevel level) {
		Registry<StructureSet> structureSets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
		Optional<Holder.Reference<StructureSet>> strongholdSet = structureSets.get(STRONGHOLD_STRUCTURE_SET);
		if (strongholdSet.isEmpty() || !(strongholdSet.get().value().placement() instanceof ConcentricRingsStructurePlacement placement)) {
			return RingData.EMPTY;
		}
		ChunkGeneratorStructureState generatorState = level.getChunkSource().getGeneratorState();
		List<ChunkPos> positions = generatorState.getRingPositionsFor(placement);
		if (positions == null || positions.isEmpty()) {
			return RingData.EMPTY;
		}
		Set<Long> strongholdChunks = new HashSet<>();
		for (ChunkPos position : positions) {
			strongholdChunks.add(position.pack());
		}
		List<Ring> rings = new ArrayList<>();
		int positionIndex = 0;
		int ringNumber = 0;
		int positionsInRing = placement.spread();
		while (positionIndex < positions.size() && positionsInRing > 0) {
			int ringEnd = Math.min(positionIndex + positionsInRing, positions.size());
			double minimumRadius = Double.MAX_VALUE;
			double maximumRadius = Double.MIN_VALUE;
			for (int index = positionIndex; index < ringEnd; index++) {
				ChunkPos position = positions.get(index);
				double radius = Math.hypot(position.x(), position.z());
				minimumRadius = Math.min(minimumRadius, radius);
				maximumRadius = Math.max(maximumRadius, radius);
			}
			rings.add(new Ring(Math.max(0.0D, minimumRadius - 0.5D), maximumRadius + 0.5D));
			positionIndex = ringEnd;
			ringNumber++;
			int remainingPositions = positions.size() - positionIndex;
			positionsInRing += 2 * positionsInRing / (ringNumber + 1);
			positionsInRing = Math.min(positionsInRing, remainingPositions);
		}
		return new RingData(List.copyOf(rings), Set.copyOf(strongholdChunks));
	}

	public record Detection(int ringIndex, boolean exactStrongholdChunk) {
		public static final Detection OUTSIDE = new Detection(-1, false);

		public boolean isInRing() {
			return this.ringIndex >= 0;
		}
	}

	private record Ring(double minimumRadius, double maximumRadius) {
		private boolean contains(ChunkPos chunkPos) {
			double radius = Math.hypot(chunkPos.x(), chunkPos.z());
			return radius >= this.minimumRadius && radius <= this.maximumRadius;
		}

		private double distanceFromCenter(ChunkPos chunkPos) {
			double radius = Math.hypot(chunkPos.x(), chunkPos.z());
			return Math.abs(radius - (this.minimumRadius + this.maximumRadius) * 0.5D);
		}
	}

	private record RingData(List<Ring> rings, Set<Long> strongholdChunks) {
		private static final RingData EMPTY = new RingData(List.of(), Set.of());

		private Detection detect(ChunkPos chunkPos) {
			int ringIndex = -1;
			double closestRingCenter = Double.MAX_VALUE;
			for (int index = 0; index < this.rings.size(); index++) {
				Ring ring = this.rings.get(index);
				if (!ring.contains(chunkPos)) {
					continue;
				}
				double distanceFromCenter = ring.distanceFromCenter(chunkPos);
				if (distanceFromCenter < closestRingCenter) {
					ringIndex = index;
					closestRingCenter = distanceFromCenter;
				}
			}
			return new Detection(ringIndex, this.strongholdChunks.contains(chunkPos.pack()));
		}
	}
}
