package com.puppy.granules.mixin.compat;

import com.puppy.granules.disc.DiscContent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper$1", remap = false)
public abstract class SophisticatedJukeboxInventoryMixin {
    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void granules$rejectBlankUnmarkedDisc(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (DiscContent.isBlank(stack)) {
            callback.setReturnValue(false);
        }
    }
}
