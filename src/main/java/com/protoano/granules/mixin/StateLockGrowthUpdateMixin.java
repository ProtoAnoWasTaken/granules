package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class StateLockGrowthUpdateMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void granules$holdGrowthAge(BlockPos pos, BlockState replacement, int flags, int recursion, CallbackInfoReturnable<Boolean> callback) {
        if (!replacement.hasProperty(StateLocks.LOCKED)) {
            return;
        }
        Level level = (Level) (Object) this;
        if (level.isClientSide()) {
            return;
        }
        BlockState existing = level.getBlockState(pos);
        if (!StateLocks.locked(existing) || !existing.is(replacement.getBlock())) {
            return;
        }
        for (var property : existing.getProperties()) {
            if (property instanceof IntegerProperty age && (age.getName().equals("age") || age.getName().equals("stage") || age.getName().equals("hatch"))
                && replacement.getValue(age) > existing.getValue(age)) {
                callback.setReturnValue(false);
                return;
            }
        }
    }
}
