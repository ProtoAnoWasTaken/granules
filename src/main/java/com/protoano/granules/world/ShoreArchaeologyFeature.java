package com.protoano.granules.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ShoreArchaeologyFeature extends Feature<NoneFeatureConfiguration> {
    public static final int RARITY = 2500;

    public ShoreArchaeologyFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int seaLevel = context.chunkGenerator().getSeaLevel();
        int startX = context.origin().getX() & ~15;
        int startZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int landTop = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                int lowestWater = Math.min(seaLevel + 1, landTop);
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    int adjacentX = x + direction.getStepX();
                    int adjacentZ = z + direction.getStepZ();
                    int waterTop = level.getHeight(Heightmap.Types.WORLD_SURFACE, adjacentX, adjacentZ);
                    if (landTop >= waterTop) {
                        lowestWater = Math.min(lowestWater, level.getHeight(Heightmap.Types.OCEAN_FLOOR, adjacentX, adjacentZ));
                    }
                }
                for (int y = Math.min(seaLevel, landTop - 1); y >= lowestWater; y--) {
                    BlockPos position = new BlockPos(x, y, z);
                    placed |= tryPlaceFind(level, position, seaLevel, context.random());
                }
            }
        }
        return placed;
    }

    public static boolean isEligible(WorldGenLevel level, BlockPos position, int seaLevel) {
        if (position.getY() > seaLevel || suspiciousVariant(level.getBlockState(position)) == null) {
            return false;
        }
        if (!level.getBlockState(position.below()).isFaceSturdy(level, position.below(), Direction.UP)) {
            return false;
        }
        int landTop = level.getHeight(Heightmap.Types.OCEAN_FLOOR, position.getX(), position.getZ());
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, position.getX(), position.getZ()) > landTop) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos water = position.relative(direction);
            if (!level.getBlockState(water).is(Blocks.WATER)) {
                continue;
            }
            int waterTop = level.getHeight(Heightmap.Types.WORLD_SURFACE, water.getX(), water.getZ());
            if (landTop < waterTop || position.getY() < level.getHeight(Heightmap.Types.OCEAN_FLOOR, water.getX(), water.getZ())) {
                continue;
            }
            for (int distance = 0; distance < 16; distance++) {
                BlockPos connectedWater = water.relative(direction, distance);
                if (!level.getBlockState(connectedWater).is(Blocks.WATER)) {
                    break;
                }
                Holder<Biome> biome = level.getNoiseBiome(QuartPos.fromBlock(connectedWater.getX()),
                    QuartPos.fromBlock(connectedWater.getY()), QuartPos.fromBlock(connectedWater.getZ()));
                if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)
                    || biome.is(BiomeTags.IS_BEACH) || biome.is(Biomes.STONY_SHORE)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryPlaceFind(WorldGenLevel level, BlockPos position, int seaLevel, RandomSource random) {
        if (!isEligible(level, position, seaLevel) || random.nextInt(com.protoano.granules.config.BalanceConfig.Setting.SHORE_ARCHAEOLOGY_DENOMINATOR.intValue()) != 0) {
            return false;
        }
        Block replacement = suspiciousVariant(level.getBlockState(position));
        if (replacement == null || !level.ensureCanWrite(position)) {
            return false;
        }
        level.setBlock(position, replacement.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(position) instanceof BrushableBlockEntity brushable) {
            brushable.setLootTable(ArchaeologyContent.SHORE_LOOT, random.nextLong());
            FeatureLocator.record(level, FeatureLocator.SHORE_ARCHAEOLOGY, position);
            return true;
        }
        return false;
    }

    private static Block suspiciousVariant(BlockState state) {
        if (state.is(Blocks.SAND)) {
            return Blocks.SUSPICIOUS_SAND;
        }
        if (state.is(Blocks.RED_SAND)) {
            return ArchaeologyContent.SUSPICIOUS_RED_SAND;
        }
        if (state.is(Blocks.GRAVEL)) {
            return Blocks.SUSPICIOUS_GRAVEL;
        }
        return null;
    }
}
