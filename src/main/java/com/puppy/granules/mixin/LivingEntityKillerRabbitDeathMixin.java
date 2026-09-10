package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.KillerRabbitAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityKillerRabbitDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void granules$sendTamedKillerRabbitDeathMessage(DamageSource source, CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (
            !(entity instanceof KillerRabbitAccess rabbit)
                || !rabbit.granules$isTamedKillerRabbit()
                || !(entity.level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)
        ) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(rabbit.granules$ownerUuid());
        if (owner != null) {
            owner.sendSystemMessage(entity.getCombatTracker().getDeathMessage());
        }
    }
}
