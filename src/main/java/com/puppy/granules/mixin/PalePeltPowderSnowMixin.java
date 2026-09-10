package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.PalePeltEquipment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PalePeltPowderSnowMixin {
    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void granules$furBootsWalkOnPowderSnow(Entity entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (entity instanceof LivingEntity living && PalePeltEquipment.wearsBoots(living)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
