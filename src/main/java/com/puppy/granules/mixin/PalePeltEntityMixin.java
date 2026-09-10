package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.PalePeltEquipment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PalePeltEntityMixin {
    @Unique
    private int granules$previousFrozenTicks;

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void granules$captureFrozenTicks(CallbackInfo callbackInfo) {
        granules$previousFrozenTicks = ((Entity) (Object) this).getTicksFrozen();
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void granules$slowTrapperHatFreezing(CallbackInfo callbackInfo) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof LivingEntity living
            && PalePeltEquipment.wearsHat(living)
            && entity.getTicksFrozen() > granules$previousFrozenTicks
            && (entity.tickCount & 1) == 0) {
            entity.setTicksFrozen(granules$previousFrozenTicks);
        }
    }

    @Inject(
        method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void granules$insulateOperaGlove(Holder<GameEvent> event, Entity source, CallbackInfo callbackInfo) {
        if ((Object) this instanceof Player player) {
            if (PalePeltEquipment.insulatesMainHand(player) && PalePeltEquipment.isHandheldEvent(event)) {
                callbackInfo.cancel();
                return;
            }
            if (PalePeltEquipment.wearsBoots(player) && (event == GameEvent.STEP || event == GameEvent.HIT_GROUND)) {
                callbackInfo.cancel();
            }
        }
    }

}
