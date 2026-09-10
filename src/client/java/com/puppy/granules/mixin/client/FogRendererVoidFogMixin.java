package com.puppy.granules.mixin.client;

import com.puppy.granules.client.VoidFogClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererVoidFogMixin {
	@Inject(method = "computeFogColor", at = @At("TAIL"))
	private void granules$darkenVoidFog(
		Camera camera,
		float partialTicks,
		ClientLevel level,
		int renderDistance,
		float darkenWorldAmount,
		Vector4f color,
		CallbackInfo callbackInfo
	) {
		float intensity = VoidFogClient.getIntensity(level, camera.position().y);
		if (intensity > 0.0F) {
			float brightness = 1.0F - intensity * 0.88F;
			color.mul(brightness, brightness, brightness, 1.0F);
		}
	}

	@Inject(method = "setupFog", at = @At("RETURN"))
	private void granules$thickenVoidFog(
		Camera camera,
		int renderDistanceInChunks,
		DeltaTracker deltaTracker,
		float darkenWorldAmount,
		ClientLevel level,
		CallbackInfoReturnable<FogData> callbackInfo
	) {
		float intensity = VoidFogClient.getIntensity(level, camera.position().y);
		if (intensity <= 0.0F) {
			return;
		}
		FogData fog = callbackInfo.getReturnValue();
		float fogEnd = Mth.lerp(intensity, fog.environmentalEnd, 8.0F);
		fog.environmentalStart = Math.min(fog.environmentalStart, fogEnd * 0.35F);
		fog.environmentalEnd = Math.min(fog.environmentalEnd, fogEnd);
		fog.renderDistanceStart = Math.min(fog.renderDistanceStart, fogEnd * 0.6F);
		fog.renderDistanceEnd = Math.min(fog.renderDistanceEnd, fogEnd);
		fog.skyEnd = Math.min(fog.skyEnd, fogEnd);
		fog.cloudEnd = Math.min(fog.cloudEnd, fogEnd);
	}
}
