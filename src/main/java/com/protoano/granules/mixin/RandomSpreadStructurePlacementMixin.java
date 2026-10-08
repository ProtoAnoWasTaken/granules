package com.protoano.granules.mixin;

import com.protoano.granules.world.StructureFrequency;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacementMixin {
	@Shadow
	@Final
	private int separation;

	@Inject(method = "isPlacementChunk", at = @At("HEAD"), cancellable = true)
	private void granules$disableStructurePlacement(ChunkGeneratorStructureState state, int chunkX, int chunkZ,
			CallbackInfoReturnable<Boolean> callback) {
		if (StructureFrequency.multiplier() == 0.0D) {
			callback.setReturnValue(false);
		}
	}

	@ModifyExpressionValue(method = {"spacing", "getPotentialStructureChunk"}, at = @At(value = "FIELD",
			target = "Lnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;spacing:I"))
	private int granules$adjustedSpacing(int original) {
		double multiplier = StructureFrequency.multiplier();
		if (multiplier == 0.0D) {
			return original;
		}
		return Math.max(this.separation + 1, (int) Math.round(original / Math.sqrt(multiplier)));
	}
}
