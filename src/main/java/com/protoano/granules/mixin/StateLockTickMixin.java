package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class StateLockTickMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void granules$pauseState(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (StateLocks.locked((BlockState) (Object) this)) {
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void granules$pauseScheduledGrowth(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        BlockState state = (BlockState) (Object) this;
        if (!StateLocks.locked(state)) {
            return;
        }
        if ((state.getBlock() instanceof net.minecraft.world.level.block.CactusBlock
            || state.getBlock() instanceof net.minecraft.world.level.block.SugarCaneBlock
            || state.getBlock() instanceof net.minecraft.world.level.block.FrogspawnBlock) && !state.canSurvive(level, pos)) {
            return;
        }
        callback.cancel();
    }
}
