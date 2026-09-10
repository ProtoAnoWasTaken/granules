package com.puppy.granules.client;

import com.puppy.granules.network.BoatJumpPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

public final class BoatJumpClientController {
	private static boolean wasJumpKeyHeld;

	private BoatJumpClientController() {
	}

	public static void initialize() {
		ClientTickEvents.END_CLIENT_TICK.register(BoatJumpClientController::tick);
	}

	private static void tick(Minecraft client) {
		if (client.player == null || !(client.player.getVehicle() instanceof AbstractBoat boat)) {
			wasJumpKeyHeld = false;
			return;
		}
		if (boat.getControllingPassenger() != client.player) {
			wasJumpKeyHeld = false;
			return;
		}
		boolean jumpKeyHeld = client.options.keyJump.isDown();
		if (wasJumpKeyHeld && !jumpKeyHeld) {
			float charge = client.player.getJumpRidingScale();
			if (charge > 0.0F) {
				ClientPlayNetworking.send(new BoatJumpPayload(boat.getId(), charge));
			}
		}
		wasJumpKeyHeld = jumpKeyHeld;
	}
}
