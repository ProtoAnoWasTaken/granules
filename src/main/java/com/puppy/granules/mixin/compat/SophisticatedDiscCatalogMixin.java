package com.puppy.granules.mixin.compat;

import com.puppy.granules.disc.DiscContent;
import java.util.List;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.VanillaDiscHandler", remap = false)
public abstract class SophisticatedDiscCatalogMixin {
    @Inject(method = "getMusicDiscs", at = @At("RETURN"), cancellable = true, remap = false)
    private void granules$excludeBlankFromRandomDiscs(CallbackInfoReturnable<List<Item>> callback) {
        List<Item> discs = callback.getReturnValue();
        if (discs.contains(DiscContent.DISC)) {
            callback.setReturnValue(discs.stream().filter(item -> item != DiscContent.DISC).toList());
        }
    }
}
