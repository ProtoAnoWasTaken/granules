package com.puppy.granules.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;

import java.util.List;
import java.util.Optional;

public final class BurrowStrongholdStructure extends Structure {
    public static final MapCodec<BurrowStrongholdStructure> CODEC = simpleCodec(BurrowStrongholdStructure::new);
    private static final ThreadLocal<Boolean> GENERATING = ThreadLocal.withInitial(() -> false);

    public BurrowStrongholdStructure(StructureSettings settings) {
        super(settings);
    }

    public static boolean isGenerating() {
        return GENERATING.get();
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos position = context.chunkPos().getWorldPosition().atY(32);
        return Optional.of(new GenerationStub(position, builder -> generatePieces(builder, context)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
        WorldgenRandom random = context.random();
        random.setLargeFeatureSeed(context.seed(), context.chunkPos().x(), context.chunkPos().z());
        StrongholdPieces.resetPieces();
        StrongholdPieces.StartPiece start = new StrongholdPieces.StartPiece(
            random,
            context.chunkPos().getBlockX(2),
            context.chunkPos().getBlockZ(2)
        );
        builder.addPiece(start);
        GENERATING.set(true);
        try {
            start.addChildren(start, builder, random);
            List<net.minecraft.world.level.levelgen.structure.StructurePiece> pending = start.pendingChildren;
            while (!pending.isEmpty()) {
                int index = random.nextInt(pending.size());
                net.minecraft.world.level.levelgen.structure.StructurePiece piece = pending.remove(index);
                piece.addChildren(start, builder, random);
            }
        } finally {
            GENERATING.remove();
        }
        int offset = 44 - builder.getBoundingBox().maxY();
        builder.offsetPiecesVertically(offset);
    }

    @Override
    public StructureType<?> type() {
        return com.puppy.granules.GranulesMod.BURROW_STRONGHOLD_STRUCTURE_TYPE;
    }
}
