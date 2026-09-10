package com.puppy.granules.world;

import com.mojang.serialization.Codec;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.rabbit.RabbitHoleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class BurrowSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public BurrowSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        long region = ((long)(origin.getX() >> 6) * 341873128712L) ^ ((long)(origin.getZ() >> 6) * 132897987541L);
        boolean forest = (region & 3L) != 0L;
        int trees = forest ? 4 + random.nextInt(6) : random.nextInt(3);
        for (int index = 0; index < trees; index++) {
            BlockPos floor = floor(level, origin.getX() + random.nextInt(16), origin.getZ() + random.nextInt(16));
            if (floor != null) {
                placeTree(level, floor.above(), random);
            }
        }
        for (int index = 0; index < 48; index++) {
            BlockPos floor = floor(level, origin.getX() + random.nextInt(16), origin.getZ() + random.nextInt(16));
            if (floor != null && level.getBlockState(floor.above()).isAir()) {
                BlockState plant = choosePlant(random);
                if (plant.canSurvive(level, floor.above())) {
                    level.setBlock(floor.above(), plant, 2);
                }
            }
        }
        if (random.nextInt(12) == 0) {
            BlockPos floor = floor(level, origin.getX() + random.nextInt(16), origin.getZ() + random.nextInt(16));
            if (floor != null && level.getBlockState(floor.above()).isAir()) {
                level.setBlock(floor.above(), RabbitHoleContent.BLOCK.defaultBlockState(), 2);
            }
        }
        if (random.nextInt(36) == 0) {
            BlockPos floor = floor(level, origin.getX() + 4 + random.nextInt(8), origin.getZ() + 4 + random.nextInt(8));
            if (floor != null) {
                placeLake(level, floor, random);
            }
        }
        return true;
    }

    private static BlockPos floor(WorldGenLevel level, int x, int z) {
        for (int y = 120; y >= 33; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static BlockState choosePlant(RandomSource random) {
        int choice = random.nextInt(20);
        if (choice == 0) {
            return GranulesMod.OLD_WORLD_ROSE.defaultBlockState();
        }
        if (choice == 1) {
            return GranulesMod.OLD_WORLD_CYAN_ROSE.defaultBlockState();
        }
        if (choice < 4) {
            return Blocks.DANDELION.defaultBlockState();
        }
        return Blocks.SHORT_GRASS.defaultBlockState();
    }

    private static void placeTree(WorldGenLevel level, BlockPos base, RandomSource random) {
        int height = 4 + random.nextInt(3);
        for (int y = 0; y <= height + 2; y++) {
            if (!level.getBlockState(base.above(y)).isAir()) {
                return;
            }
        }
        BlockState log = random.nextInt(4) == 0
            ? Blocks.BIRCH_LOG.defaultBlockState()
            : Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = (log.is(Blocks.BIRCH_LOG) ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES)
            .defaultBlockState()
            .setValue(LeavesBlock.PERSISTENT, true);
        for (int y = 0; y < height; y++) {
            level.setBlock(base.above(y), log, 2);
        }
        for (int y = height - 2; y <= height + 1; y++) {
            int radius = y >= height ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius && Math.abs(z) == radius && random.nextBoolean()) {
                        continue;
                    }
                    BlockPos leaf = base.offset(x, y, z);
                    if (level.getBlockState(leaf).isAir()) {
                        level.setBlock(leaf, leaves, 2);
                    }
                }
            }
        }
    }

    private static void placeLake(WorldGenLevel level, BlockPos center, RandomSource random) {
        int radius = 2 + random.nextInt(2);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) {
                    continue;
                }
                BlockPos surface = center.offset(x, 0, z);
                level.setBlock(surface, Blocks.WATER.defaultBlockState(), 2);
                if (Math.abs(x) < radius && Math.abs(z) < radius) {
                    level.setBlock(surface.below(), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(surface.below(2), Blocks.CLAY.defaultBlockState(), 2);
                }
            }
        }
    }
}
