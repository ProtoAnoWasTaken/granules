package com.puppy.granules.mixin;

import com.puppy.granules.advancement.GranulesAdvancements;
import com.puppy.granules.rabbit.KillerRabbitAccess;
import com.puppy.granules.rabbit.RabbitHoleContent;
import com.puppy.granules.config.ContentManifest;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.Comparator;
import java.util.EnumSet;

@Mixin(Rabbit.class)
public abstract class RabbitMixin implements KillerRabbitAccess {
    @Unique
    private static final Identifier GRANULES_TAMED_HEALTH = Identifier.fromNamespaceAndPath("granules", "tamed_killer_rabbit_health");
    @Unique
    private static final Identifier GRANULES_TAMED_ARMOR = Identifier.fromNamespaceAndPath("granules", "tamed_killer_rabbit_armor");
    @Unique
    private static final EntityDataAccessor<String> GRANULES_OWNER = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.STRING);
    @Unique
    private static final EntityDataAccessor<Boolean> GRANULES_SITTING = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private boolean granules$palePeltExcluded;
    @Unique
    private UUID granules$owner;
    @Unique
    private boolean granules$orderedToSit;
    @Unique
    private int granules$hungerCooldown;
    @Unique
    private int granules$tamingProgress;
    @Unique
    private int granules$tamingTarget;
    @Unique
    private UUID granules$defenseTarget;
    @Unique
    private int granules$lastRabbitHurtTimestamp;
    @Unique
    private int granules$lastOwnerAttackTimestamp;
    @Unique
    private int granules$lastOwnerHurtTimestamp;

    @Override
    public boolean granules$dropsPalePelt() {
        Rabbit rabbit = (Rabbit) (Object) this;
        return !ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)
            && !granules$palePeltExcluded
            && rabbit.getVariant() == Rabbit.Variant.EVIL;
    }

    @Override
    public boolean granules$isTamedKillerRabbit() {
        Rabbit rabbit = (Rabbit) (Object) this;
        return !ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)
            && rabbit.getVariant() == Rabbit.Variant.EVIL
            && granules$ownerUuid() != null;
    }

    @Override
    public UUID granules$ownerUuid() {
        Rabbit rabbit = (Rabbit) (Object) this;
        String synchronizedOwner = rabbit.getEntityData().get(GRANULES_OWNER);
        if (!synchronizedOwner.isEmpty()) {
            try {
                return UUID.fromString(synchronizedOwner);
            } catch (IllegalArgumentException exception) {
                return granules$owner;
            }
        }
        return granules$owner;
    }

    @Override
    public void granules$setOwnerUuid(UUID ownerUuid) {
        granules$owner = ownerUuid;
        ((Rabbit) (Object) this).getEntityData().set(GRANULES_OWNER, ownerUuid == null ? "" : ownerUuid.toString());
        granules$defenseTarget = null;
        Rabbit rabbit = (Rabbit) (Object) this;
        granules$lastRabbitHurtTimestamp = rabbit.getLastHurtByMobTimestamp();
        if (ownerUuid != null && rabbit.level() instanceof ServerLevel level && level.getEntity(ownerUuid) instanceof LivingEntity owner) {
            granules$lastOwnerAttackTimestamp = owner.getLastHurtMobTimestamp();
            granules$lastOwnerHurtTimestamp = owner.getLastHurtByMobTimestamp();
        }
        rabbit.setTarget(null);
        rabbit.getNavigation().stop();
        if (ownerUuid != null) {
            granules$applyTamedAttributes(rabbit, true);
        }
    }

    @Unique
    private void granules$applyTamedAttributes(Rabbit rabbit, boolean restoreHealth) {
        AttributeInstance health = rabbit.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance armor = rabbit.getAttribute(Attributes.ARMOR);
        if (health != null) {
            health.addOrReplacePermanentModifier(
                new AttributeModifier(GRANULES_TAMED_HEALTH, 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }
        if (armor != null) {
            armor.addOrReplacePermanentModifier(
                new AttributeModifier(GRANULES_TAMED_ARMOR, 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }
        if (restoreHealth) {
            rabbit.setHealth(rabbit.getMaxHealth());
        }
    }

    @Override
    public boolean granules$isOrderedToSit() {
        return ((Rabbit) (Object) this).getEntityData().get(GRANULES_SITTING);
    }

    @Override
    public void granules$toggleSitting(Player player) {
        if (player.getUUID().equals(granules$ownerUuid())) {
            granules$setOrderedToSit(!granules$isOrderedToSit());
            granules$defenseTarget = null;
            Rabbit rabbit = (Rabbit) (Object) this;
            rabbit.setTarget(null);
            rabbit.getNavigation().stop();
        }
    }

    @Override
    public boolean granules$allowsTarget(LivingEntity target) {
        return target != (Object) this
            && !granules$isOrderedToSit()
            && granules$defenseTarget != null
            && granules$defenseTarget.equals(target.getUUID());
    }

    @Override
    public int granules$feedingProgress() {
        return granules$tamingProgress;
    }

    @Override
    public int granules$feedingTarget() {
        return granules$tamingTarget;
    }

    @Override
    public int granules$feedingCooldown() {
        return granules$hungerCooldown;
    }

    @Unique
    private void granules$setOrderedToSit(boolean sitting) {
        granules$orderedToSit = sitting;
        ((Rabbit) (Object) this).getEntityData().set(GRANULES_SITTING, sitting);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void granules$defineKillerRabbitState(SynchedEntityData.Builder builder, CallbackInfo callbackInfo) {
        builder.define(GRANULES_OWNER, "");
        builder.define(GRANULES_SITTING, false);
    }

    @Inject(method = "setVariant", at = @At("TAIL"))
    private void granules$removeAutomaticKillerName(Rabbit.Variant variant, CallbackInfo callbackInfo) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
			return;
		}
        Rabbit rabbit = (Rabbit) (Object) this;
        if (
            variant == Rabbit.Variant.EVIL
                && rabbit.getCustomName() != null
                && rabbit.getCustomName().getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("entity.minecraft.killer_bunny")
        ) {
            rabbit.setCustomName(null);
        }
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void granules$registerKillerRabbitSitGoal(CallbackInfo callbackInfo) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
			return;
		}
        Rabbit rabbit = (Rabbit) (Object) this;
        Goal sitGoal = new Goal() {
            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
            }

            @Override
            public boolean canUse() {
                return granules$isTamedKillerRabbit() && granules$isOrderedToSit();
            }

            @Override
            public boolean canContinueToUse() {
                return canUse();
            }

            @Override
            public void start() {
                rabbit.setTarget(null);
                rabbit.getNavigation().stop();
            }

            @Override
            public void tick() {
                rabbit.setTarget(null);
                rabbit.getNavigation().stop();
            }
        };
        rabbit.getGoalSelector().addGoal(0, sitGoal);
        Goal followOwnerGoal = new Goal() {
            private LivingEntity followedOwner;
            private int pathRecalculationDelay;

            {
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                followedOwner = granules$getLivingOwner(rabbit);
                return followedOwner != null
                    && !granules$isOrderedToSit()
                    && granules$defenseTarget == null
                    && rabbit.distanceToSqr(followedOwner) > 36.0D;
            }

            @Override
            public boolean canContinueToUse() {
                return followedOwner != null
                    && followedOwner.isAlive()
                    && !granules$isOrderedToSit()
                    && granules$defenseTarget == null
                    && rabbit.distanceToSqr(followedOwner) > 9.0D;
            }

            @Override
            public void start() {
                pathRecalculationDelay = 0;
            }

            @Override
            public void stop() {
                followedOwner = null;
                rabbit.getNavigation().stop();
            }

            @Override
            public void tick() {
                if (--pathRecalculationDelay <= 0) {
                    pathRecalculationDelay = adjustedTickDelay(10);
                    rabbit.getNavigation().moveTo(followedOwner, 1.1D);
                }
            }
        };
        rabbit.getGoalSelector().addGoal(5, followOwnerGoal);
    }

    @Unique
    private LivingEntity granules$getLivingOwner(Rabbit rabbit) {
        UUID ownerUuid = granules$ownerUuid();
        if (ownerUuid == null || !(rabbit.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntity(ownerUuid) instanceof LivingEntity owner ? owner : null;
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void granules$handleKillerRabbitTaming(ServerLevel level, CallbackInfo callbackInfo) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
			return;
		}
        Rabbit rabbit = (Rabbit) (Object) this;
        if (rabbit.getVariant() != Rabbit.Variant.EVIL) {
            return;
        }
        if (granules$hungerCooldown > 0) {
            granules$hungerCooldown--;
        }
        if (granules$owner == null && granules$hungerCooldown == 0 && rabbit.getTarget() == null) {
            granules$seekDroppedMeat(rabbit, level);
        }
        if (granules$owner == null) {
            if (granules$hungerCooldown > 0) {
                rabbit.setTarget(null);
                rabbit.getNavigation().stop();
            }
            return;
        }
        granules$updateTamedTarget(rabbit, level);
    }

    @Unique
    private void granules$seekDroppedMeat(Rabbit rabbit, ServerLevel level) {
        ItemEntity food = level.getEntitiesOfClass(ItemEntity.class, rabbit.getBoundingBox().inflate(16.0D)).stream()
            .filter(item -> item.getOwner() instanceof Player)
            .filter(item -> granules$nutrition(item.getItem()) > 0)
            .min(Comparator.comparingDouble(rabbit::distanceToSqr))
            .orElse(null);
        if (food == null) {
            return;
        }
        if (rabbit.distanceToSqr(food) <= 4.0D) {
            granules$consumeDroppedMeat(rabbit, level);
            return;
        }
        rabbit.getNavigation().moveTo(food, 1.2D);
    }

    @Unique
    private void granules$consumeDroppedMeat(Rabbit rabbit, ServerLevel level) {
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, rabbit.getBoundingBox().inflate(2.0D))) {
            if (!(itemEntity.getOwner() instanceof Player player)) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            ItemStack eaten = stack.copyWithCount(1);
            int nutrition = granules$nutrition(stack);
            if (nutrition == 0) {
                continue;
            }
            if (granules$tamingTarget == 0) {
                granules$tamingTarget = 8 + level.getRandom().nextInt(7);
            }
            granules$tamingProgress += nutrition;
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
            granules$hungerCooldown = 2400 + level.getRandom().nextInt(3601);
            rabbit.setTarget(null);
            rabbit.getNavigation().stop();
            level.playSound(null, rabbit.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.HOSTILE, 1.6F, 0.65F);
            level.sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM, eaten.getItem()),
                rabbit.getX(),
                rabbit.getY() + 0.35D,
                rabbit.getZ(),
                48,
                0.35D,
                0.3D,
                0.35D,
                0.16D
            );
			if (granules$tamingProgress >= granules$tamingTarget) {
				granules$setOwnerUuid(player.getUUID());
				granules$setOrderedToSit(false);
				if (player instanceof ServerPlayer serverPlayer) {
					GranulesAdvancements.award(serverPlayer, "freak_on_a_leash");
				}
                level.sendParticles(ParticleTypes.HEART, rabbit.getX(), rabbit.getY() + 0.6D, rabbit.getZ(), 12, 0.45D, 0.4D, 0.45D, 0.08D);
            } else {
                level.sendParticles(ParticleTypes.SMOKE, rabbit.getX(), rabbit.getY() + 0.5D, rabbit.getZ(), 7, 0.3D, 0.3D, 0.3D, 0.03D);
            }
            return;
        }
    }

    @Unique
    private int granules$nutrition(ItemStack stack) {
        if (stack.is(Items.RABBIT_STEW)) {
            return 10;
        }
        if (stack.is(Items.ROTTEN_FLESH)) {
            return 4;
        }
        if (stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.RABBIT)) {
            return 3;
        }
        if (stack.is(Items.CHICKEN) || stack.is(Items.MUTTON) || stack.is(Items.COD) || stack.is(Items.SALMON)) {
            return 2;
        }
        if (stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
            return 1;
        }
        return 0;
    }

    @Unique
    private void granules$updateTamedTarget(Rabbit rabbit, ServerLevel level) {
        if (granules$isOrderedToSit()) {
            granules$defenseTarget = null;
            rabbit.setTarget(null);
            rabbit.getNavigation().stop();
            return;
        }
        LivingEntity owner = level.getEntity(granules$owner) instanceof LivingEntity living ? living : null;
        LivingEntity target = level.getEntity(granules$defenseTarget) instanceof LivingEntity living ? living : null;
        if (target != null && target.isAlive()) {
            rabbit.setTarget(target);
            return;
        }
        granules$defenseTarget = null;
        target = granules$findNewDefenseTarget(rabbit, owner);
        if (target == null || target == owner) {
            rabbit.setTarget(null);
            return;
        }
        boolean directlyAttackedRabbit = target == rabbit.getLastHurtByMob();
        if (target instanceof TamableAnimal tamable && tamable.isTame() && !directlyAttackedRabbit) {
            rabbit.setTarget(null);
            return;
        }
        granules$defenseTarget = target.getUUID();
        rabbit.setTarget(target);
    }

    @Unique
    private LivingEntity granules$findNewDefenseTarget(Rabbit rabbit, LivingEntity owner) {
        int rabbitHurtTimestamp = rabbit.getLastHurtByMobTimestamp();
        if (rabbitHurtTimestamp != granules$lastRabbitHurtTimestamp) {
            granules$lastRabbitHurtTimestamp = rabbitHurtTimestamp;
            return rabbit.getLastHurtByMob();
        }
        if (owner == null) {
            return null;
        }
        int ownerAttackTimestamp = owner.getLastHurtMobTimestamp();
        if (ownerAttackTimestamp != granules$lastOwnerAttackTimestamp) {
            granules$lastOwnerAttackTimestamp = ownerAttackTimestamp;
            return owner.getLastHurtMob();
        }
        int ownerHurtTimestamp = owner.getLastHurtByMobTimestamp();
        if (ownerHurtTimestamp != granules$lastOwnerHurtTimestamp) {
            granules$lastOwnerHurtTimestamp = ownerHurtTimestamp;
            return owner.getLastHurtByMob();
        }
        return null;
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void granules$chooseKillerRabbit(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        EntitySpawnReason reason,
        SpawnGroupData groupData,
        CallbackInfoReturnable<SpawnGroupData> callbackInfo
    ) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
			return;
		}
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            Rabbit rabbit = (Rabbit) (Object) this;
            if (level.getLevel().dimension().equals(RabbitHoleContent.BURROW)) {
                if (rabbit.getY() < 48.0D && level.getRandom().nextInt(100) == 0) {
                    ((RabbitAccessor) this).granules$setVariant(Rabbit.Variant.EVIL);
                }
                return;
            }
            if (level.getRandom().nextInt(2500) == 0) {
                ((RabbitAccessor) this).granules$setVariant(Rabbit.Variant.EVIL);
            }
            return;
        }
        if (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER) {
            granules$palePeltExcluded = true;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void granules$savePalePeltEligibility(ValueOutput output, CallbackInfo callbackInfo) {
        output.putBoolean("GranulesPalePeltExcluded", granules$palePeltExcluded);
        if (granules$owner != null) {
            output.store("GranulesKillerRabbitOwner", net.minecraft.core.UUIDUtil.CODEC, granules$owner);
        }
        output.putBoolean("GranulesKillerRabbitSitting", granules$isOrderedToSit());
        output.putInt("GranulesKillerRabbitHungerCooldown", granules$hungerCooldown);
        output.putInt("GranulesKillerRabbitTamingProgress", granules$tamingProgress);
        output.putInt("GranulesKillerRabbitTamingTarget", granules$tamingTarget);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void granules$loadPalePeltEligibility(ValueInput input, CallbackInfo callbackInfo) {
        granules$palePeltExcluded = input.getBooleanOr("GranulesPalePeltExcluded", granules$palePeltExcluded);
        granules$setOwnerUuid(input.read("GranulesKillerRabbitOwner", net.minecraft.core.UUIDUtil.CODEC).orElse(null));
        if (granules$ownerUuid() != null) {
            granules$applyTamedAttributes((Rabbit) (Object) this, false);
        }
        granules$setOrderedToSit(input.getBooleanOr("GranulesKillerRabbitSitting", false));
        granules$hungerCooldown = input.getIntOr("GranulesKillerRabbitHungerCooldown", 0);
        granules$tamingProgress = input.getIntOr("GranulesKillerRabbitTamingProgress", 0);
        granules$tamingTarget = input.getIntOr("GranulesKillerRabbitTamingTarget", 0);
    }
}
