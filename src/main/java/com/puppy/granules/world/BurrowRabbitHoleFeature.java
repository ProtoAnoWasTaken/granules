package com.puppy.granules.world;

import com.mojang.serialization.Codec;
import com.puppy.granules.rabbit.RabbitHoleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class BurrowRabbitHoleFeature extends Feature<NoneFeatureConfiguration> {
    public BurrowRabbitHoleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos ground = origin.below();
        if ((!level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
            && !level.getBlockState(ground).is(Blocks.PODZOL)
            && !level.getBlockState(ground).is(Blocks.MYCELIUM))
            || !level.getBlockState(origin).isAir()) {
            return false;
        }
        level.setBlock(origin, RabbitHoleContent.BLOCK.defaultBlockState(), 2);
        return true;
    }
}
