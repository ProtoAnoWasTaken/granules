package com.protoano.granules.mixin;

import com.protoano.granules.enchantment.Hearthearth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class HearthearthRespawnMixin {
    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void respawnAtCampfire(boolean keepSpawnBlock, TeleportTransition.PostTeleportTransition afterTeleport,
                                   CallbackInfoReturnable<TeleportTransition> callback) {
        var player = (ServerPlayer) (Object) this;
        var config = player.getRespawnConfig();
        if (config == null || config.forced()) {
            return;
        }
        var data = config.respawnData();
        var level = player.level().getServer().getLevel(data.dimension());
        if (level == null) {
            return;
        }
        Hearthearth.respawnPosition(level, data.pos(), player.getUUID()).ifPresent(position -> {
            float yaw = (float) Math.toDegrees(Math.atan2(data.pos().getZ() + 0.5 - position.z,
                data.pos().getX() + 0.5 - position.x)) - 90.0F;
            callback.setReturnValue(new TeleportTransition(level, position, Vec3.ZERO, yaw, 0.0F, afterTeleport));
        });
    }
}
