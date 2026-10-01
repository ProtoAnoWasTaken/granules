package com.protoano.granules.mixin;

import com.protoano.granules.golem.GolemCreeperFilter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class GolemCreeperFilterMixin {
    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void granules$acquireCreeper(CallbackInfo callback) {
        GolemCreeperFilter.tick((Mob) (Object) this);
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void granules$recordCreeperTarget(LivingEntity target, CallbackInfo callback) {
        GolemCreeperFilter.recordAggression((Mob) (Object) this);
    }
}
