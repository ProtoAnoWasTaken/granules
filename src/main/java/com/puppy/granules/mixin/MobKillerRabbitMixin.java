package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.KillerRabbitAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobKillerRabbitMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity granules$rejectKillerRabbitOwnerTarget(LivingEntity target) {
        if (granules$isDisallowedTarget(target)) {
            return null;
        }
        return target;
    }

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void granules$cannotAttackKillerRabbitOwner(LivingEntity target, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (granules$isDisallowedTarget(target)) {
            callbackInfo.setReturnValue(false);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void granules$cannotDamageKillerRabbitOwner(
        ServerLevel level,
        Entity target,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (target instanceof LivingEntity living && granules$isDisallowedTarget(living)) {
            callbackInfo.setReturnValue(false);
        }
    }

    private boolean granules$isDisallowedTarget(LivingEntity target) {
        if (!((Object) this instanceof KillerRabbitAccess rabbit) || !rabbit.granules$isTamedKillerRabbit()) {
            return false;
        }
        return target != null && !rabbit.granules$allowsTarget(target);
    }
}
