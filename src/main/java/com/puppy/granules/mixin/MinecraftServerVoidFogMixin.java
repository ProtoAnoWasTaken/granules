package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerVoidFogMixin {
	@Inject(method = "onGameRuleChanged", at = @At("TAIL"))
	private void granules$syncVoidFog(GameRule<?> rule, Object value, CallbackInfo callbackInfo) {
		if (rule != GranulesMod.VOID_FOG) {
			return;
		}
		MinecraftServer server = (MinecraftServer) (Object) this;
		boolean enabled = (Boolean) value;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			GranulesMod.sendVoidFogState(player, enabled);
		}
	}
}
