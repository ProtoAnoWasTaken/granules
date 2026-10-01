package com.protoano.granules.mixin;

import com.protoano.granules.minecart.MinecartCoupling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Leashable.class)
public interface MinecartLeashTickMixin {
    @Inject(method = "tickLeash", at = @At("HEAD"), cancellable = true)
    private static <E extends Entity & Leashable> void granules$tickCoupledCart(
        ServerLevel level, E entity, CallbackInfo callback
    ) {
        if (entity instanceof MinecartCoupling coupling && coupling.granules$getCoupledCart() != null) {
            coupling.granules$tickCoupling();
            callback.cancel();
        }
    }
}
