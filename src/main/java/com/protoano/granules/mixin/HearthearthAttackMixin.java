package com.protoano.granules.mixin;

import com.protoano.granules.enchantment.Hearthearth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class HearthearthAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void preventExhaustedAttack(Entity target, CallbackInfo callback) {
        if (Hearthearth.isExhausted(((Player) (Object) this).getMainHandItem())) {
            callback.cancel();
        }
    }

    @Inject(method = "cannotAttackWithItem", at = @At("HEAD"), cancellable = true)
    private void markSwordUnavailable(ItemStack stack, int ticks, CallbackInfoReturnable<Boolean> callback) {
        if (Hearthearth.isExhausted(stack)) {
            callback.setReturnValue(true);
        }
    }
}
