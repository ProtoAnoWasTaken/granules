package com.protoano.granules.mixin;

import com.protoano.granules.world.FeatureLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConfiguredFeature.class)
public abstract class ConfiguredFeatureLocatorMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void granules$recordPlacement(WorldGenLevel level, ChunkGenerator generator, RandomSource random,
        BlockPos position, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue()) {
            FeatureLocator.record(level, (ConfiguredFeature<?, ?>) (Object) this, position);
        }
    }
}
