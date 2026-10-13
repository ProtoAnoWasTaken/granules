package com.protoano.granules.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.portal.TeleportTransition;

public final class ReturnToSpawnEffect extends MobEffect {
    public ReturnToSpawnEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7C3D8F);
    }

    @Override
    public boolean isInstantaneous() {
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        returnToSpawn(entity, amplifier);
        return false;
    }

    @Override
    public void applyInstantaneousEffect(ServerLevel level, Entity source, Entity indirectSource,
                                         LivingEntity entity, int amplifier, double strength) {
        returnToSpawn(entity, amplifier);
    }

    private static void returnToSpawn(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        TeleportTransition destination = player.findRespawnPositionAndUseSpawnBlock(
            true, TeleportTransition.PLACE_PORTAL_TICKET);
        if (amplifier == 0 && destination.newLevel() != player.level()) {
            return;
        }
        player.stopRiding();
        player.fallDistance = 0;
        player.teleport(destination);
    }
}
