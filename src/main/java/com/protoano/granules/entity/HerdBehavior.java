package com.protoano.granules.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

public final class HerdBehavior {
    private HerdBehavior() {
    }

    public interface Member {
        Controller granules$herd();
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (damage > 0 && entity instanceof PathfinderMob mob) {
                alarm(mob, source);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof PathfinderMob mob) {
                alarm(mob, source);
            }
        });
    }

    private static void alarm(PathfinderMob mob, net.minecraft.world.damagesource.DamageSource source) {
        if (!canParticipate(mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Entity attacker = source.getEntity();
        Vec3 danger = attacker == null ? mob.position() : attacker.position();
        for (PathfinderMob neighbor : level.getEntitiesOfClass(PathfinderMob.class,
            mob.getBoundingBox().inflate(com.protoano.granules.config.BalanceConfig.Setting.HERD_ALERT_RADIUS.value()), candidate -> candidate.getType() == mob.getType() && eligible(candidate))) {
            ((Member) neighbor).granules$herd().alert(level, danger, attacker);
        }
        ((Member) mob).granules$herd().alert(level, danger, attacker);
    }

    public static boolean eligible(PathfinderMob mob) {
        return mob.isAlive() && canParticipate(mob);
    }

    private static boolean canParticipate(PathfinderMob mob) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.HERD_BEHAVIOR)) {
            return false;
        }
        if (!supported(mob.getType()) || mob.isNoAi() || mob.isLeashed() || mob.isVehicle() || mob.isPassenger()) {
            return false;
        }
        if (mob instanceof Llama llama) {
            return !llama.inCaravan() && !llama.hasCaravanTail() && llama.getTarget() == null;
        }
        return !(mob instanceof AbstractHorse horse) || !horse.isTamed();
    }

    public static void install(PathfinderMob mob, Controller controller) {
        if (!supported(mob.getType())) {
            return;
        }
        mob.getGoalSelector().addGoal(1, new FleeGoal(mob, controller));
        mob.getGoalSelector().addGoal(4, new GatherGoal(mob, controller));
    }

    private static boolean supported(EntityType<?> type) {
        return type == EntityTypes.PIG || type == EntityTypes.COW || type == EntityTypes.SHEEP
            || type == EntityTypes.HORSE || type == EntityTypes.DONKEY || type == EntityTypes.MULE
            || type == EntityTypes.LLAMA || type == EntityTypes.TRADER_LLAMA;
    }

    public static final class Controller {
        private Vec3 danger;
        private UUID attacker;
        private long calmAt;

        public void alert(ServerLevel level, Vec3 position, Entity source) {
            danger = position;
            attacker = source == null ? null : source.getUUID();
            calmAt = level.getGameTime() + com.protoano.granules.config.BalanceConfig.Setting.HERD_CALM_TICKS.intValue();
        }

        public boolean threatened(PathfinderMob mob) {
            if (danger == null || !(mob.level() instanceof ServerLevel level)) {
                return false;
            }
            Entity source = attacker == null ? null : level.getEntity(attacker);
            if (source != null && source.isAlive() && mob.distanceToSqr(source) <= Math.pow(com.protoano.granules.config.BalanceConfig.Setting.HERD_ALERT_RADIUS.value(), 2)) {
                danger = source.position();
                calmAt = level.getGameTime() + com.protoano.granules.config.BalanceConfig.Setting.HERD_CALM_TICKS.intValue();
            }
            if (level.getGameTime() >= calmAt) {
                danger = null;
                attacker = null;
                return false;
            }
            return true;
        }
    }

    private static final class FleeGoal extends Goal {
        private final PathfinderMob mob;
        private final Controller controller;
        private int repath;

        private FleeGoal(PathfinderMob mob, Controller controller) {
            this.mob = mob;
            this.controller = controller;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return eligible(mob) && controller.threatened(mob);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath > 0) {
                return;
            }
            repath = 10;
            if (mob.position().distanceToSqr(controller.danger) < Math.pow(com.protoano.granules.config.BalanceConfig.Setting.HERD_ALERT_RADIUS.value(), 2)) {
                Vec3 destination = DefaultRandomPos.getPosAway(mob, 12, 7, controller.danger);
                if (destination != null) {
                    mob.getNavigation().moveTo(destination.x, destination.y, destination.z, com.protoano.granules.config.BalanceConfig.Setting.HERD_FLEE_SPEED.value());
                }
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }

    private static final class GatherGoal extends Goal {
        private final PathfinderMob mob;
        private final Controller controller;
        private PathfinderMob leader;
        private int repath;
        private long nextSearch;

        private GatherGoal(PathfinderMob mob, Controller controller) {
            this.mob = mob;
            this.controller = controller;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            leader = null;
            if (!eligible(mob) || controller.threatened(mob) || mob.level().getGameTime() < nextSearch) {
                return false;
            }
            nextSearch = mob.level().getGameTime() + 20;
            leader = mob.level().getEntitiesOfClass(PathfinderMob.class, mob.getBoundingBox().inflate(com.protoano.granules.config.BalanceConfig.Setting.HERD_GATHER_RADIUS.value()),
                candidate -> candidate.getType() == mob.getType() && candidate.getId() < mob.getId()
                    && eligible(candidate) && !((Member) candidate).granules$herd().threatened(candidate))
                .stream().min(Comparator.comparingInt(Entity::getId)).orElse(null);
            if (leader == null || mob.distanceToSqr(leader) <= 36) {
                leader = null;
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return leader != null && eligible(mob) && eligible(leader) && !controller.threatened(mob)
                && !((Member) leader).granules$herd().threatened(leader)
                && mob.distanceToSqr(leader) > 16
                && mob.distanceToSqr(leader) < Math.pow(com.protoano.granules.config.BalanceConfig.Setting.HERD_GATHER_RADIUS.value() + 8, 2);
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void tick() {
            if (--repath <= 0) {
                repath = 10;
                mob.getNavigation().moveTo(leader, com.protoano.granules.config.BalanceConfig.Setting.HERD_GATHER_SPEED.value());
            }
        }

        @Override
        public void stop() {
            leader = null;
            mob.getNavigation().stop();
        }
    }
}
