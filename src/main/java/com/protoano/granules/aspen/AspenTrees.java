package com.protoano.granules.aspen;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class AspenTrees {
    public static final int BIRCH_REPLACEMENT_CHANCE = 64;

    private AspenTrees() {
    }

    public static int sampleSize(RandomSource random) {
        return Math.clamp((int) Math.round(6.0 + random.nextGaussian() * 0.65), 4, 7);
    }

    public static boolean placeNormal(LevelAccessor level, BlockPos base, RandomSource random) {
        for (int size = sampleSize(random); size >= 4; size--) {
            if (place(level, base, normalShape(size), 1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean placeMega(LevelAccessor level, BlockPos base) {
        return place(level, base, megaShape(), 2);
    }

    public static Map<BlockPos, Boolean> normalShape(int size) {
        if (size < 4 || size > 7) {
            throw new IllegalArgumentException("Aspen size must be between 4 and 7");
        }
        Map<BlockPos, Boolean> shape = new LinkedHashMap<>();
        int trunk = size == 7 ? 8 : size;
        int start = size == 6 || size == 5 ? 2 : 1;
        for (int y = 0; y < trunk; y++) {
            shape.put(new BlockPos(0, y, 0), true);
        }
        for (int y = start; y < trunk; y++) {
            boolean corners = size == 7 ? (y - 1) % 3 != 1 : size == 4 ? y < 3 : y % 2 == 0;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if ((x != 0 || z != 0) && (corners || Math.abs(x) + Math.abs(z) == 1)) {
                        shape.put(new BlockPos(x, y, z), false);
                    }
                }
            }
        }
        shape.put(new BlockPos(0, trunk, 0), false);
        if (size == 7) {
            shape.put(new BlockPos(1, trunk, 0), false);
            shape.put(new BlockPos(-1, trunk, 0), false);
            shape.put(new BlockPos(0, trunk, 1), false);
            shape.put(new BlockPos(0, trunk, -1), false);
        } else {
            shape.put(new BlockPos(0, trunk + 1, 0), false);
        }
        return shape;
    }

    public static Map<BlockPos, Boolean> megaShape() {
        Map<BlockPos, Boolean> shape = new LinkedHashMap<>();
        for (int y = 0; y < 14; y++) {
            int radius = y < 4 ? 2 : y <= 10 ? 1 : 0;
            for (int x = -radius; x <= 1 + radius; x++) {
                for (int z = -radius; z <= 1 + radius; z++) {
                    int dx = Math.max(-x, x - 1);
                    int dz = Math.max(-z, z - 1);
                    boolean trunk = dx <= 0 && dz <= 0;
                    if (trunk && y < 10) {
                        shape.put(new BlockPos(x, y, z), true);
                    } else if (y >= 2
                        && !((y == 3 || y >= 8 && y <= 10) && dx == radius && dz == radius)) {
                        shape.put(new BlockPos(x, y, z), false);
                    }
                }
            }
        }
        return shape;
    }

    private static boolean place(LevelAccessor level, BlockPos base, Map<BlockPos, Boolean> shape, int width) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
                if (!level.getBlockState(base.offset(x, -1, z)).is(BlockTags.SUPPORTS_VEGETATION)) {
                    return false;
                }
            }
        }
        for (var entry : shape.entrySet()) {
            BlockPos pos = base.offset(entry.getKey());
            BlockState existing = level.getBlockState(pos);
            boolean sapling = entry.getKey().getY() == 0 && existing.is(AspenContent.SAPLING);
            if (level.isOutsideBuildHeight(pos) || !level.getFluidState(pos).isEmpty()
                || !(existing.isAir() || existing.is(BlockTags.REPLACEABLE_BY_TREES) || sapling)) {
                return false;
            }
        }
        for (var entry : shape.entrySet()) {
            BlockState state = AspenContent.LOG.defaultBlockState();
            if (!entry.getValue()) {
                int distance = 7;
                for (var log : shape.entrySet()) {
                    if (log.getValue()) {
                        distance = Math.min(distance, entry.getKey().distManhattan(log.getKey()));
                    }
                }
                state = AspenContent.LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, distance);
            }
            level.setBlock(base.offset(entry.getKey()), state, 3);
        }
        return true;
    }
}
