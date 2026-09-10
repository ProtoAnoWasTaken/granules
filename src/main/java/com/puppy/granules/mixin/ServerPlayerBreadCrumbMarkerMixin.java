package com.puppy.granules.mixin;

import com.puppy.granules.world.BreadCrumbMarkerAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerBreadCrumbMarkerMixin {
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void granules$copyBreadCrumbMarker(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo callbackInfo) {
		BreadCrumbMarkerAccess oldAccess = (BreadCrumbMarkerAccess) oldPlayer;
		BreadCrumbMarkerAccess newAccess = (BreadCrumbMarkerAccess) this;
		newAccess.granules$setBreadCrumbMarker(oldAccess.granules$getBreadCrumbMarker());
	}
}
