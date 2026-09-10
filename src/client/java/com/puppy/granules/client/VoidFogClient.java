package com.puppy.granules.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import com.puppy.granules.rabbit.RabbitHoleContent;

public final class VoidFogClient {
	private static final double FULL_INTENSITY_Y = -59.0D;
	private static final double FADE_OUT_Y = 16.0D;
	private static final float INTENSITY_MULHEADLIER = 1.5F;
	private static boolean enabled = true;

	private VoidFogClient() {
	}

	public static void setEnabled(boolean enabled) {
		VoidFogClient.enabled = enabled;
	}

	public static float getIntensity(ClientLevel level, double y) {
		boolean burrow = level.dimension().equals(RabbitHoleContent.BURROW);
		if ((!enabled && !burrow) || !burrow && level.dimension() != Level.OVERWORLD || y >= (burrow ? 48.0D : FADE_OUT_Y)) {
			return 0.0F;
		}
		double fullIntensityY = burrow ? -32.0D : FULL_INTENSITY_Y;
		double fadeOutY = burrow ? 48.0D : FADE_OUT_Y;
		if (y <= fullIntensityY) {
			return 1.0F;
		}
		double linearIntensity = (fadeOutY - y) / (fadeOutY - fullIntensityY);
		float intensity = (float) (Math.expm1(linearIntensity * 3.0D) / Math.expm1(3.0D));
		float multiplier = burrow ? 2.0F : INTENSITY_MULHEADLIER;
		return Math.min(1.0F, intensity * multiplier);
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null) {
			return;
		}
		float intensity = getIntensity(minecraft.level, minecraft.player.getY());
		if (intensity <= 0.0F) {
			return;
		}
		int particleCount = Math.max(1, (int) Math.ceil(intensity * 6.0F));
		for (int particle = 0; particle < particleCount; particle++) {
			double x = minecraft.player.getX() + (minecraft.level.getRandom().nextDouble() - 0.5D) * 16.0D;
			double y = minecraft.player.getY() + minecraft.level.getRandom().nextDouble() * 6.0D - 4.0D;
			double z = minecraft.player.getZ() + (minecraft.level.getRandom().nextDouble() - 0.5D) * 16.0D;
			minecraft.level.addParticle(ParticleTypes.ASH, x, y, z, 0.0D, 0.002D, 0.0D);
		}
	}
}
