package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.PalePeltEquipment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class PalePeltLevelMixin {
    @Inject(
        method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void granules$insulateOperaGlove(
        Holder<GameEvent> event,
        Vec3 position,
        GameEvent.Context context,
        CallbackInfo callbackInfo
    ) {
        if (context.sourceEntity() instanceof Player player
            && PalePeltEquipment.insulatesMainHand(player)
            && PalePeltEquipment.isHandheldEvent(event)) {
            callbackInfo.cancel();
            return;
        }
        if (context.sourceEntity() instanceof LivingEntity living
            && PalePeltEquipment.wearsBoots(living)
            && (event == GameEvent.STEP || event == GameEvent.HIT_GROUND)) {
            callbackInfo.cancel();
        }
    }
}
