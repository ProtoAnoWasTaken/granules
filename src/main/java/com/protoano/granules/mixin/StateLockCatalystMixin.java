package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkCatalystBlockEntity.class)
public abstract class StateLockCatalystMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void granules$pauseSpread(Level level, BlockPos pos, BlockState state, SculkCatalystBlockEntity catalyst, CallbackInfo callback) {
        if (StateLocks.locked(state)) {
            callback.cancel();
        }
    }
}
