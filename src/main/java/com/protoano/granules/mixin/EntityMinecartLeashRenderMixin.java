package com.protoano.granules.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMinecartLeashRenderMixin {
	@Inject(method = "getRopeHoldPosition", at = @At("RETURN"), cancellable = true)
	private void anchorLeashesAtMinecartCenter(float partialTick, CallbackInfoReturnable<Vec3> callback) {
		Entity entity = (Entity) (Object) this;
		if (entity instanceof AbstractMinecart minecart) {
			callback.setReturnValue(minecart.getPosition(partialTick).add(0.0D, minecart.getBbHeight() * 0.5D, 0.0D));
		}
	}
}
