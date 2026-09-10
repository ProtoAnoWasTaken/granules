package com.puppy.granules.rabbit;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public interface KillerRabbitAccess {
    boolean granules$dropsPalePelt();

    boolean granules$isTamedKillerRabbit();

    UUID granules$ownerUuid();

    void granules$setOwnerUuid(UUID ownerUuid);

    boolean granules$isOrderedToSit();

    void granules$toggleSitting(Player player);

    boolean granules$allowsTarget(LivingEntity target);

    int granules$feedingProgress();

    int granules$feedingTarget();

    int granules$feedingCooldown();
}
