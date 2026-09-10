package com.puppy.granules.mixin;

import com.puppy.granules.world.BurrowStrongholdStructure;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StrongholdPieces.class)
public abstract class BurrowStrongholdPortalMixin {
    @Inject(method = "findAndCreatePieceFactory", at = @At("HEAD"), cancellable = true)
    private static void granules$excludePortalRoom(
        Class<?> pieceClass,
        StructurePieceAccessor accessor,
        RandomSource random,
        int x,
        int y,
        int z,
        net.minecraft.core.Direction direction,
        int depth,
        CallbackInfoReturnable<net.minecraft.world.level.levelgen.structure.StructurePiece> callback
    ) {
        if (BurrowStrongholdStructure.isGenerating() && pieceClass == StrongholdPieces.PortalRoom.class) {
            callback.setReturnValue(null);
        }
    }
}
