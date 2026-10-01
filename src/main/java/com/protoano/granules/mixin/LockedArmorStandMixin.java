package com.protoano.granules.mixin;

import com.protoano.granules.item.EntityLocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class LockedArmorStandMixin {
    @Inject(method = "canUseSlot", at = @At("HEAD"), cancellable = true)
    private void granules$denyDispensers(EquipmentSlot slot, CallbackInfoReturnable<Boolean> callback) {
        if (EntityLocks.locked((ArmorStand) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
