package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ProtectionCurseMixin {
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F"), cancellable = true)
    private void applyNegativeProtection(DamageSource source, float amount, CallbackInfoReturnable<Float> callback, @Local(ordinal = 1) float protection) {
        if (protection < 0.0F) {
            callback.setReturnValue(amount * (1.0F - Math.max(-20.0F, protection) / 25.0F));
        }
    }
}
