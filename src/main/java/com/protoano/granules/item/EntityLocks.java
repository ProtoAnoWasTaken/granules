package com.protoano.granules.item;

import com.protoano.granules.sound.GranulesSounds;
import com.protoano.granules.mixin.CopperGolemLockAccessor;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;

public final class EntityLocks {
    public static final String LOCKED = "granules:locked_display";

    private EntityLocks() {
    }

    public static boolean supports(Entity entity) {
        return entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof CopperGolem;
    }

    public static boolean locked(Entity entity) {
        return entity.entityTags().contains(LOCKED);
    }

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!supports(entity) || !player.getItemInHand(hand).is(UtilityItems.IRON_LOCK) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            if (!level.mayInteract(player, entity.blockPosition()) || !player.getAbilities().mayBuild) {
                return InteractionResult.FAIL;
            }
            if (entity instanceof CopperGolem golem) {
                if (!level.getBlockState(golem.blockPosition()).isAir()) {
                    return InteractionResult.FAIL;
                }
                if (level instanceof ServerLevel serverLevel) {
                    ((CopperGolemLockAccessor) golem).granules$turnToStatue(serverLevel);
                    com.protoano.granules.advancement.GranulesAdvancements.recordLock(player, true);
                    player.getItemInHand(hand).hurtAndBreak(1, player, hand);
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                boolean locked = locked(entity);
                if (locked) {
                    entity.removeTag(LOCKED);
                } else if (!entity.addTag(LOCKED)) {
                    return InteractionResult.FAIL;
                }
                entity.playSound(locked ? GranulesSounds.UNLOCK : GranulesSounds.LOCK, 1.0F, 1.0F);
                com.protoano.granules.advancement.GranulesAdvancements.recordLock(player, !locked);
                player.getItemInHand(hand).hurtAndBreak(1, player, hand);
            }
            return InteractionResult.SUCCESS;
        });
    }
}
