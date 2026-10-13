package com.protoano.granules.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompassAngleState.class)
public abstract class NorthCompassMixin {
    @Shadow
    @Final
    private CompassAngleState.CompassTarget compassTarget;

    @Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
    private void granules$pointNorth(ItemStack stack, ClientLevel level, int seed, ItemOwner owner,
                                     CallbackInfoReturnable<Float> callback) {
        if (compassTarget == CompassAngleState.CompassTarget.SPAWN) {
            float rotation = owner.getVisualRotationYInDegrees() / 360.0F;
            callback.setReturnValue(Mth.positiveModulo(0.5F - rotation, 1.0F));
        }
    }
}
