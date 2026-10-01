package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.protoano.granules.enchantment.Necrosoles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class NecrosolesPhysicsMixin {
    @Inject(method = "shouldTravelInFluid", at = @At("HEAD"), cancellable = true)
    private void useGroundPhysics(FluidState fluid, CallbackInfoReturnable<Boolean> callback) {
        if (Necrosoles.active((LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
    private boolean useGroundJump(boolean original) {
        return original && !Necrosoles.active((LivingEntity) (Object) this);
    }
}
