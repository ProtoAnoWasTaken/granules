package com.puppy.granules.mixin;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolem.class)
public abstract class IronGolemFilterMixin {
    @Unique
    private UUID granules$playerTarget;

    @Unique
    private boolean granules$creepers;

    @Unique
    private boolean granules$piglins;

    @Unique
    private boolean granules$endermen;

    @Unique
    private boolean granules$raiderVision;

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void configureFilters(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        IronGolem self = (IronGolem) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        boolean handled = false;
        boolean removed = false;
        if (stack.is(Items.PLAYER_HEAD)) {
            ResolvableProfile profile = stack.get(DataComponents.PROFILE);
            if (profile != null) {
                UUID playerId = profile.partialProfile().id();
                removed = playerId.equals(granules$playerTarget);
                granules$playerTarget = removed ? null : playerId;
                handled = true;
            }
        } else if (stack.is(Items.CREEPER_HEAD)) {
            granules$creepers = !granules$creepers;
            removed = !granules$creepers;
            handled = true;
        } else if (stack.is(Items.PIGLIN_HEAD)) {
            granules$piglins = !granules$piglins;
            removed = !granules$piglins;
            handled = true;
        } else if (stack.is(Items.ENDER_EYE)) {
            granules$endermen = !granules$endermen;
            removed = !granules$endermen;
            handled = true;
        } else if (self.isPlayerCreated() && granules$isOminousBanner(self, stack)) {
            granules$raiderVision = !granules$raiderVision;
            removed = !granules$raiderVision;
            handled = true;
        }
        if (!handled) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        LivingEntity current = self.getTarget();
        if (current != null && !granules$matches(self, current)) {
            self.setTarget(null);
        }
        if (self.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                removed ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER,
                self.getX(),
                self.getY() + self.getBbHeight() * 0.75D,
                self.getZ(),
                removed ? 12 : 8,
                0.45D,
                0.5D,
                0.45D,
                0.02D
            );
        }
        callback.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void allowFilteredTargets(LivingEntity target, CallbackInfoReturnable<Boolean> callback) {
        IronGolem self = (IronGolem) (Object) this;
        if (granules$matches(self, target)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void acquireFilteredTarget(CallbackInfo callback) {
        IronGolem self = (IronGolem) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || self.tickCount % 10 != 0) {
            return;
        }
        LivingEntity current = self.getTarget();
        if (current != null && current.isAlive()) {
            return;
        }
        LivingEntity target = level.getEntitiesOfClass(
            LivingEntity.class,
            self.getBoundingBox().inflate(32.0D),
            candidate -> granules$matches(self, candidate) && granules$canSee(self, candidate)
        ).stream().min(Comparator.comparingDouble(self::distanceToSqr)).orElse(null);
        if (target != null) {
            self.setTarget(target);
        }
    }

    @Unique
    private boolean granules$matches(IronGolem self, LivingEntity candidate) {
        if (candidate instanceof Player player) {
            return granules$playerTarget != null && granules$playerTarget.equals(player.getUUID());
        }
        if (granules$creepers && candidate.getType() == EntityTypes.CREEPER) {
            return true;
        }
        if (granules$piglins && (
            candidate.getType() == EntityTypes.PIGLIN
                || candidate.getType() == EntityTypes.PIGLIN_BRUTE
                || candidate.getType() == EntityTypes.ZOMBIFIED_PIGLIN
                || candidate.getType() == EntityTypes.HOGLIN
                || candidate.getType() == EntityTypes.ZOGLIN
        )) {
            return true;
        }
        if (granules$endermen && candidate.getType() == EntityTypes.ENDERMAN) {
            return true;
        }
        return granules$raiderVision && self.isPlayerCreated() && candidate instanceof Raider;
    }

    @Unique
    private boolean granules$canSee(IronGolem self, LivingEntity candidate) {
        if (granules$raiderVision && self.isPlayerCreated() && candidate instanceof Raider) {
            return true;
        }
        return self.getSensing().hasLineOfSight(candidate);
    }

    @Unique
    private static boolean granules$isOminousBanner(IronGolem golem, ItemStack stack) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return false;
        }
        ItemStack ominous = Raid.getOminousBannerInstance(level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN));
        return ItemStack.isSameItemSameComponents(stack, ominous);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveFilters(ValueOutput output, CallbackInfo callback) {
        if (granules$playerTarget != null) {
            output.putString("GranulesPlayerTarget", granules$playerTarget.toString());
        }
        output.putBoolean("GranulesCreepers", granules$creepers);
        output.putBoolean("GranulesPiglins", granules$piglins);
        output.putBoolean("GranulesEndermen", granules$endermen);
        output.putBoolean("GranulesRaiderVision", granules$raiderVision);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadFilters(ValueInput input, CallbackInfo callback) {
        granules$playerTarget = input.getString("GranulesPlayerTarget").map(value -> {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }).orElse(null);
        granules$creepers = input.getBooleanOr("GranulesCreepers", false);
        granules$piglins = input.getBooleanOr("GranulesPiglins", false);
        granules$endermen = input.getBooleanOr("GranulesEndermen", false);
        granules$raiderVision = input.getBooleanOr("GranulesRaiderVision", false);
    }
}
