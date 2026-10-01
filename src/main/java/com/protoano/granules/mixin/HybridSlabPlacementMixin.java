package com.protoano.granules.mixin;

import com.protoano.granules.block.HybridSlabs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class HybridSlabPlacementMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void granules$combineSlabs(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = HybridSlabs.tryCombine((BlockItem) (Object) this, context);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}
