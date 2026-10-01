package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepeaterBlock.class)
public abstract class StateLockRepeaterMixin {
    @Inject(method = "isLocked", at = @At("HEAD"), cancellable = true)
    private void granules$holdLocked(LevelReader level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> callback) {
        if (StateLocks.locked(state)) {
            callback.setReturnValue(true);
        }
    }
}
