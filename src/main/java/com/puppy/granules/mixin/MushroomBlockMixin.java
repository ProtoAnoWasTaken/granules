package com.puppy.granules.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomBlock.class)
public abstract class MushroomBlockMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void granules$allowBrightGroundPlacement(
        BlockState state,
        LevelReader level,
        BlockPos pos,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.PODZOL)) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void granules$preventBrightGrowth(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        CallbackInfo callbackInfo
    ) {
        if (level.getMaxLocalRawBrightness(pos) >= 13) {
            callbackInfo.cancel();
        }
    }
}
