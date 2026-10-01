package com.protoano.granules.aspen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;

public final class AspenLeavesBlock extends LeavesBlock {
    public static final MapCodec<AspenLeavesBlock> CODEC = simpleCodec(AspenLeavesBlock::new);
    public static final int PARTICLE_COLOR = 0xFFB1A21C;

    public AspenLeavesBlock(Properties properties) {
        super(0.01F, properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random, ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, PARTICLE_COLOR));
    }
}
