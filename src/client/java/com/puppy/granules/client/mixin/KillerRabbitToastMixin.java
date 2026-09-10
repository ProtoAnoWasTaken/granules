package com.puppy.granules.client.mixin;

import com.puppy.granules.rabbit.KillerRabbitAccess;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RabbitRenderer.class)
public abstract class KillerRabbitToastMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void granules$preserveKillerRabbitTexture(
        Rabbit rabbit,
        RabbitRenderState state,
        float partialTick,
        CallbackInfo callback
    ) {
        if (rabbit.getVariant() == Rabbit.Variant.EVIL) {
            state.isToast = false;
        }
        if (
            rabbit instanceof KillerRabbitAccess access
                && access.granules$isTamedKillerRabbit()
                && access.granules$isOrderedToSit()
        ) {
            state.jumpCompletion = 0.0F;
            state.hopAnimationState.stop();
            state.idleHeadTiltAnimationState.stop();
        }
    }
}
