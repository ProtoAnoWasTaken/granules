package com.puppy.granules.mixin;

import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin implements PlayerRideableJumping {
	@Override
	public void onPlayerJump(int jumpPower) {
	}

	@Override
	public boolean canJump() {
		AbstractBoat boat = (AbstractBoat) (Object) this;
		return boat.getControllingPassenger() != null && (boat.onGround() || boat.isInWater() || boat.isUnderWater());
	}

	@Override
	public void handleStartJump(int jumpPower) {
	}

	@Override
	public void handleStopJump() {
	}
}
