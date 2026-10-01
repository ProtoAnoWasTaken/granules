package com.protoano.granules.mixin;

import com.protoano.granules.golem.CopperGolemUpgrades;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolem.class)
public class CopperGolemUpgradesMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void granules$upgrade(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var result = CopperGolemUpgrades.interact((CopperGolem) (Object) this, player, hand);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void granules$tickUpgrades(CallbackInfo callback) {
        CopperGolemUpgrades.tick((CopperGolem) (Object) this);
    }
}
