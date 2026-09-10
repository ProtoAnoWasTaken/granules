package com.puppy.granules.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class RabbitFallDamageMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void granules$protectRabbitsFromFalls(
        double fallDistance,
        float damageMultiplier,
        DamageSource source,
        CallbackInfoReturnable<Boolean> callback
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (
            entity instanceof Rabbit
                && !entity.level().getBlockState(entity.getOnPos()).is(Blocks.POINTED_DRIPSTONE)
                && !entity.level().getBlockState(entity.blockPosition().below()).is(Blocks.POINTED_DRIPSTONE)
        ) {
            callback.setReturnValue(false);
        }
    }
}
