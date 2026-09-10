package com.puppy.granules.client.mixin;

import com.puppy.granules.client.GlowSheepRenderStateAccess;
import com.puppy.granules.world.GlowSheepAccess;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepRenderer.class)
public abstract class SheepRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void granules$extractGlowWoolState(Sheep sheep, SheepRenderState renderState, float partialTick, CallbackInfo callbackInfo) {
		((GlowSheepRenderStateAccess) renderState).granules$setGlowWool(((GlowSheepAccess) sheep).granules$hasGlowWool());
	}
}
