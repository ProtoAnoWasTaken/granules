package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.PalePeltEquipment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PalePeltLivingEntityMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float granules$reduceKineticDamage(float amount, ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.FLY_INTO_WALL) && PalePeltEquipment.wearsHat((LivingEntity) (Object) this)) {
            return amount * 0.5F;
        }
        return amount;
    }
}
