package com.protoano.granules.mixin;

import com.protoano.granules.entity.HostileDeterrence;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class HostileDeterrenceMixin {
    @Unique
    private final HostileDeterrence.Controller granules$deterrence = new HostileDeterrence.Controller();

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void fleeDeterrents(CallbackInfo callback) {
        if (granules$deterrence.tick((Mob) (Object) this)) {
            callback.cancel();
        }
    }
}
