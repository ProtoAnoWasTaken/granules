package com.puppy.granules.mixin;

import com.puppy.granules.disc.DiscContent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlankDiscMixin {
    @Inject(method = "setTheItem", at = @At("HEAD"), cancellable = true)
    private void granules$rejectBlankUnmarkedDisc(ItemStack stack, CallbackInfo callback) {
        if (!DiscContent.isBlank(stack)) {
            return;
        }
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity)(Object)this;
        if (jukebox.getLevel() != null && !jukebox.getLevel().isClientSide()) {
            Block.popResource(jukebox.getLevel(), jukebox.getBlockPos().above(), stack.copy());
        }
        callback.cancel();
    }
}
