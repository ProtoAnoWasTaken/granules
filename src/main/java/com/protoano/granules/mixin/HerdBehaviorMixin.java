package com.protoano.granules.mixin;

import com.protoano.granules.entity.HerdBehavior;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public abstract class HerdBehaviorMixin implements HerdBehavior.Member {
    @Unique
    private final HerdBehavior.Controller granules$herd = new HerdBehavior.Controller();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void installHerdGoals(CallbackInfo callback) {
        HerdBehavior.install((PathfinderMob) (Object) this, granules$herd);
    }

    @Override
    public HerdBehavior.Controller granules$herd() {
        return granules$herd;
    }
}
