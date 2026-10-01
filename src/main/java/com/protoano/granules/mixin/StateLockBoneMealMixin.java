package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public abstract class StateLockBoneMealMixin {
    @Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
    private static void granules$denyGrowth(ItemStack stack, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (StateLocks.locked(level.getBlockState(pos))) {
            callback.setReturnValue(false);
        }
    }
}
