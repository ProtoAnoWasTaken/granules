package com.protoano.granules.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public abstract class FurnaceMinecartSpeedMixin {
    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void granules$tripleSpeed(ServerLevel level, CallbackInfoReturnable<Double> callback) {
        callback.setReturnValue(callback.getReturnValue() * 3.0D);
    }
}
