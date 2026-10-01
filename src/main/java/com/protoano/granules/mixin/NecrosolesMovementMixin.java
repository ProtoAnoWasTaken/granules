package com.protoano.granules.mixin;

import com.protoano.granules.enchantment.Necrosoles;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class NecrosolesMovementMixin {
    @Inject(method = "isSwimming", at = @At("RETURN"), cancellable = true)
    private void preventSwimming(CallbackInfoReturnable<Boolean> callback) {
        Player self = (Player) (Object) this;
        if (Necrosoles.active(self)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
    private void controlUnderwaterMovement(CallbackInfo callback) {
        Player self = (Player) (Object) this;
        if (!Necrosoles.active(self)) {
            return;
        }
        self.setSwimming(false);
        callback.cancel();
    }
}
