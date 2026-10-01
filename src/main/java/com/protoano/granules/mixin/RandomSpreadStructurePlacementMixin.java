package com.protoano.granules.mixin;

import com.protoano.granules.world.StructureFrequency;
import com.protoano.granules.mixin.access.StructurePlacementAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacementMixin {
	@Shadow
	@Final
	private int spacing;

	@Shadow
	@Final
	private int separation;

	@Shadow
	@Final
	private RandomSpreadType spreadType;

	@Overwrite
	public int spacing() {
		return this.granules$adjustedSpacing();
	}

	@Overwrite
	public ChunkPos getPotentialStructureChunk(long levelSeed, int chunkX, int chunkZ) {
		int adjustedSpacing = this.granules$adjustedSpacing();
		int regionX = Math.floorDiv(chunkX, adjustedSpacing);
		int regionZ = Math.floorDiv(chunkZ, adjustedSpacing);
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(levelSeed, regionX, regionZ, ((StructurePlacementAccessor) (Object) this).granules$getSalt());
		int range = adjustedSpacing - separation;
		int potentialX = regionX * adjustedSpacing + spreadType.evaluate(random, range);
		int potentialZ = regionZ * adjustedSpacing + spreadType.evaluate(random, range);
		return new ChunkPos(potentialX, potentialZ);
	}

	@Overwrite
	protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		if (StructureFrequency.multiplier() == 0.0D) {
			return false;
		}
		ChunkPos potentialChunk = this.getPotentialStructureChunk(state.getLevelSeed(), chunkX, chunkZ);
		return potentialChunk.x() == chunkX && potentialChunk.z() == chunkZ;
	}

	private int granules$adjustedSpacing() {
		double multiplier = StructureFrequency.multiplier();
		if (multiplier == 0.0D) {
			return this.spacing;
		}
		return Math.max(this.separation + 1, (int) Math.round(this.spacing / Math.sqrt(multiplier)));
	}
}
