package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SaplingBlock.class)
public abstract class StateLockSaplingMixin {
    @Inject(method = "advanceTree", at = @At("HEAD"), cancellable = true)
    private void granules$keepSapling(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, CallbackInfo callback) {
        if (StateLocks.treeLocked(level, pos, state)) {
            callback.cancel();
        }
    }
}
