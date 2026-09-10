package com.puppy.granules.client;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.block.CasterBlock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CasterDebugBeamRenderer {
	private CasterDebugBeamRenderer() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(CasterDebugBeamRenderer::renderPreview);
	}

	private static void renderPreview(Minecraft client) {
		if (client.player == null || client.level == null || !client.player.isShiftKeyDown()) {
			return;
		}
		if (!(client.hitResult instanceof BlockHitResult hitResult)) {
			return;
		}

		Level level = client.level;
		BlockPos pos = hitResult.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!state.is(GranulesMod.CASTER)) {
			return;
		}

		Direction facing = state.getValue(CasterBlock.FACING);
		int rearSignal = level.getSignal(pos.relative(facing.getOpposite()), facing);
		if (rearSignal > 0 || !isDebugActive(state)) {
			return;
		}

		int range = state.getValue(CasterBlock.OUTPUT_POWER);
		if (range == 0) {
			range = 15;
		}
		Vec3 start = CasterBlock.getRayStart(pos, facing);
		Vec3 end = CasterBlock.getRayEnd(level, pos, facing, range);
		Vec3 ray = end.subtract(start);
		if (ray.lengthSqr() <= 0.0025D) {
			return;
		}

		for (int particle = 0; particle < 8; particle++) {
			Vec3 particlePos = start.add(ray.scale(level.getRandom().nextDouble()));
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

	private static boolean isDebugActive(BlockState state) {
		return state.getValue(CasterBlock.POWERED)
			|| state.getValue(CasterBlock.TRIGGERED)
			|| state.getValue(CasterBlock.POWER) > 0;
	}

}
