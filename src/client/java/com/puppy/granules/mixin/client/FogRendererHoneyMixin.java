package com.puppy.granules.mixin.client;

import com.puppy.granules.GranulesMod;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererHoneyMixin {
	@Inject(method = "computeFogColor", at = @At("TAIL"))
	private void granules$colorHoneyFog(
		Camera camera,
		float partialTicks,
		ClientLevel level,
		int renderDistance,
		float darkenWorldAmount,
		Vector4f color,
		CallbackInfo callbackInfo
	) {
		if (isCameraInHoney(camera, level)) {
			color.set(0.61F, 0.36F, 0.05F, 1.0F);
		}
	}

	@Inject(method = "setupFog", at = @At("RETURN"))
	private void granules$thickenHoneyFog(
		Camera camera,
		int renderDistanceInChunks,
		DeltaTracker deltaTracker,
		float darkenWorldAmount,
		ClientLevel level,
		CallbackInfoReturnable<FogData> callbackInfo
	) {
		if (!isCameraInHoney(camera, level)) {
			return;
		}
		FogData fog = callbackInfo.getReturnValue();
		fog.environmentalStart = 0.25F;
		fog.environmentalEnd = 7.0F;
		fog.renderDistanceStart = 2.0F;
		fog.renderDistanceEnd = 7.0F;
		fog.skyEnd = 7.0F;
		fog.cloudEnd = 7.0F;
	}

	private static boolean isCameraInHoney(Camera camera, ClientLevel level) {
		BlockPos pos = camera.blockPosition();
		FluidState fluidState = level.getFluidState(pos);
		return fluidState.getType().isSame(GranulesMod.HONEY) && camera.position().y < pos.getY() + fluidState.getHeight(level, pos);
	}
}
