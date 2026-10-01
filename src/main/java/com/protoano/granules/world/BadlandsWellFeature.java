package com.protoano.granules.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BadlandsWellFeature extends Feature<NoneFeatureConfiguration> {
    public BadlandsWellFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin().above();
        while (level.isEmptyBlock(origin) && origin.getY() > level.getMinY() + 2) {
            origin = origin.below();
        }
        if (!level.getBlockState(origin).is(Blocks.RED_SAND)) {
            return false;
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (level.isEmptyBlock(origin.offset(x, -1, z)) && level.isEmptyBlock(origin.offset(x, -2, z))) {
                    return false;
                }
            }
        }
        for (int y = -2; y <= 0; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    set(level, origin.offset(x, y, z), Blocks.RED_SANDSTONE);
                }
            }
        }
        List<BlockPos> pool = new ArrayList<>();
        pool.add(origin);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            pool.add(origin.relative(direction));
        }
        for (BlockPos position : pool) {
            set(level, position, Blocks.WATER);
            set(level, position.below(), Blocks.RED_SAND);
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    set(level, origin.offset(x, 1, z), Blocks.RED_SANDSTONE);
                }
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            set(level, origin.relative(direction, 2).above(), Blocks.RED_SANDSTONE_SLAB);
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block roof = x == 0 && z == 0 ? Blocks.RED_SANDSTONE : Blocks.RED_SANDSTONE_SLAB;
                set(level, origin.offset(x, 4, z), roof);
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int x = -1; x <= 1; x += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    set(level, origin.offset(x, y, z), Blocks.RED_SANDSTONE);
                }
            }
        }
        for (int depth = 1; depth <= 2; depth++) {
            BlockPos position = pool.get(context.random().nextInt(pool.size())).below(depth);
            set(level, position, ArchaeologyContent.SUSPICIOUS_RED_SAND);
            if (level.getBlockEntity(position) instanceof BrushableBlockEntity brushable) {
                brushable.setLootTable(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, context.random().nextLong());
            }
        }
        return true;
    }

    private static void set(WorldGenLevel level, BlockPos position, Block block) {
        level.setBlock(position, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
