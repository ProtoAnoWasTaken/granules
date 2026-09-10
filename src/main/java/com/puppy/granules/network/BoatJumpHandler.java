package com.puppy.granules.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

public final class BoatJumpHandler {
	private BoatJumpHandler() {
	}

	public static void handle(ServerPlayer player, BoatJumpPayload payload) {
		if (!(player.getVehicle() instanceof AbstractBoat boat)) {
			return;
		}
		if (boat.getId() != payload.boatId() || boat.getControllingPassenger() != player) {
			return;
		}
		if (!canJump(boat)) {
			return;
		}
		double charge = Math.max(0.0D, Math.min(1.0D, payload.charge()));
		double jumpVelocity = 0.2D + charge * 0.15D;
		Vec3 velocity = boat.getDeltaMovement();
		boat.setDeltaMovement(velocity.x, Math.max(velocity.y, jumpVelocity), velocity.z);
		boat.setOnGround(false);
		boat.hurtMarked = true;
	}

	private static boolean canJump(AbstractBoat boat) {
		return boat.onGround() || boat.isInWater() || boat.isUnderWater();
	}
}
