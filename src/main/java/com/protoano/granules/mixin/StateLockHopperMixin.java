package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlock.class)
public abstract class StateLockHopperMixin {
    @Inject(method = "checkPoweredState", at = @At("HEAD"), cancellable = true)
    private void granules$holdDisabled(Level level, BlockPos pos, BlockState state, CallbackInfo callback) {
        if (StateLocks.locked(state)) {
            if (state.getValue(HopperBlock.ENABLED)) {
                level.setBlock(pos, state.setValue(HopperBlock.ENABLED, false), 2);
            }
            callback.cancel();
        }
    }
}
