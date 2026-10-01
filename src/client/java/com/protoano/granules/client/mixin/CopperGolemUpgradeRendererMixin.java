package com.protoano.granules.client.mixin;

import com.protoano.granules.client.golem.GolemEyeTextures;
import com.protoano.granules.client.golem.GolemUpgradeRenderState;
import com.protoano.granules.golem.CopperGolemUpgrades;
import java.util.function.Function;
import net.minecraft.client.renderer.entity.CopperGolemRenderer;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolemRenderer.class)
public class CopperGolemUpgradeRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/golem/CopperGolem;Lnet/minecraft/client/renderer/entity/state/CopperGolemRenderState;F)V", at = @At("TAIL"))
    private void granules$extractUpgrades(CopperGolem entity, CopperGolemRenderState state, float partialTick, CallbackInfo callback) {
        ((GolemUpgradeRenderState) state).granules$eyes(entity.getAttachedOrElse(CopperGolemUpgrades.UPGRADES, CopperGolemUpgrades.Upgrades.NONE).eyes());
    }

    @Inject(method = "getEyeTextureLocationProvider", at = @At("RETURN"), cancellable = true)
    private static void granules$eyeTextures(CallbackInfoReturnable<Function<CopperGolemRenderState, Identifier>> callback) {
        var original = callback.getReturnValue();
        callback.setReturnValue(state -> {
            Identifier upgraded = GolemEyeTextures.upgraded(state);
            return upgraded == null ? original.apply(state) : upgraded;
        });
    }
}
