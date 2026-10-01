package com.protoano.granules.mixin;

import com.protoano.granules.bomb.BombContent;
import com.protoano.granules.bomb.TntEffects;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntPowerMixin {
    @ModifyArg(
        method = "explode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
        ),
        index = 2
    )
    private net.minecraft.world.level.ExplosionDamageCalculator granules$protectTerrain(net.minecraft.world.level.ExplosionDamageCalculator original) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        if (tnt.getBlockState().getBlock() instanceof com.protoano.granules.bomb.GranulesTntBlock) {
            return com.protoano.granules.bomb.TntEffects.DAMAGE_CALCULATOR;
        }
        return original;
    }

    @ModifyArg(
        method = "explode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
        ),
        index = 6
    )
    private float granules$adjustExplosionPower(float power) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        if (tnt.getBlockState().is(BombContent.DIRT_TNT)) {
            return 8.0F;
        }
        if (tnt.getBlockState().is(BombContent.DRY_TNT)) {
            return 4.0F;
        }
        return power;
    }

    @Inject(method = "explode", at = @At("TAIL"))
    private void granules$applyBombEffects(CallbackInfo callback) {
        TntEffects.apply((PrimedTnt) (Object) this);
    }
}
