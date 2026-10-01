package com.protoano.granules.aspen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

public final class AspenSaplingBlock extends SaplingBlock {
    public static final MapCodec<AspenSaplingBlock> CODEC = simpleCodec(AspenSaplingBlock::new);

    public AspenSaplingBlock(Properties properties) {
        super(TreeGrower.BIRCH, properties);
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0) {
            advanceTree(level, pos, state, random);
        }
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (com.protoano.granules.item.StateLocks.treeLocked(level, pos, state)) {
            return;
        }
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.setValue(STAGE, 1), 4);
            return;
        }
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                BlockPos corner = pos.offset(x, 0, z);
                if (isSquare(level, corner)) {
                    AspenTrees.placeMega(level, corner);
                    return;
                }
            }
        }
        AspenTrees.placeNormal(level, pos, random);
    }

    private boolean isSquare(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(this)
            && level.getBlockState(pos.east()).is(this)
            && level.getBlockState(pos.south()).is(this)
            && level.getBlockState(pos.east().south()).is(this);
    }
}
