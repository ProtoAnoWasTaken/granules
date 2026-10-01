package com.protoano.granules.mixin;

import com.protoano.granules.config.ContentManifest;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class ManifestDispenserMixin {
    @Inject(method = "getDispenseMethod", at = @At("HEAD"), cancellable = true)
    private void blockBannedItems(Level level, ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> callback) {
        if (ContentManifest.get().isBanned(stack.getItem())) {
            callback.setReturnValue(DispenseItemBehavior.NOOP);
        }
    }
}
