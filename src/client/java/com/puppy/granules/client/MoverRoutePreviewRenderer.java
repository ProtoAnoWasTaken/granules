package com.puppy.granules.client;

import com.puppy.granules.entity.MoverEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class MoverRoutePreviewRenderer {
	private MoverRoutePreviewRenderer() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(MoverRoutePreviewRenderer::renderPreview);
	}

	private static void renderPreview(Minecraft client) {
		if (client.player == null || client.level == null || !client.player.isShiftKeyDown()) {
			return;
		}
		if (!(client.hitResult instanceof EntityHitResult hitResult)) {
			return;
		}
		if (!(hitResult.getEntity() instanceof MoverEntity mover) || !mover.isPreviewingRoute()) {
			return;
		}

		List<Vec3> routePoints = mover.getRoutePreviewPoints();
		if (routePoints.isEmpty()) {
			return;
		}
		Level level = client.level;
		Vec3 segmentStart = mover.position();
		for (Vec3 segmentEnd : routePoints) {
			Vec3 segment = segmentEnd.subtract(segmentStart);
			if (segment.lengthSqr() > 0.0025D) {
				for (int particle = 0; particle < 8; particle++) {
					Vec3 particlePos = segmentStart.add(segment.scale(level.getRandom().nextDouble()));
					level.addParticle(
						DustParticleOptions.REDSTONE,
						particlePos.x,
						particlePos.y,
						particlePos.z,
						0.0D,
						0.0D,
						0.0D
					);
				}
			}
			segmentStart = segmentEnd;
		}
	}
}
