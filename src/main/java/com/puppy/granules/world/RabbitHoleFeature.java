package com.puppy.granules.world;

import com.mojang.serialization.Codec;
import com.puppy.granules.rabbit.RabbitHoleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RabbitHoleFeature extends Feature<NoneFeatureConfiguration> {
    public RabbitHoleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos ground = origin.below();
        int surfaceY = ground.getY();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos check = ground.offset(x, 0, z);
                BlockState surface = level.getBlockState(check);
                if ((!surface.is(Blocks.GRASS_BLOCK) && !surface.is(Blocks.PODZOL) && !surface.is(Blocks.MYCELIUM))
                    || !level.getBlockState(check.above()).isAir()
                    || check.getY() != surfaceY) {
                    return false;
                }
            }
        }
        level.setBlock(origin, RabbitHoleContent.BLOCK.defaultBlockState(), 2);
        RandomSource random = context.random();
        List<BlockPos> ring = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if ((Math.abs(x) == 2 || Math.abs(z) == 2) && !(Math.abs(x) == 2 && Math.abs(z) == 2)) {
                    ring.add(origin.offset(x, 0, z));
                }
            }
        }
        Collections.shuffle(ring, new java.util.Random(random.nextLong()));
        int mushroomCount = 1 + random.nextInt(6);
        for (int index = 0; index < mushroomCount; index++) {
            BlockState mushroom = random.nextBoolean()
                ? Blocks.RED_MUSHROOM.defaultBlockState()
                : Blocks.BROWN_MUSHROOM.defaultBlockState();
            level.setBlock(ring.get(index), mushroom, 2);
        }
        return true;
    }
}
