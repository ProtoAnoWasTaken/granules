package com.protoano.granules.entity;

import com.protoano.granules.mixin.PhantomFlightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.JukeboxSongs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class HostileDeterrence {
    private HostileDeterrence() {
    }

    public static boolean fearsCampfires(Mob mob) {
        return mob.getType() != EntityTypes.BEE
            && (mob.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)
                || mob.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD));
    }

    public static double campfireRadius(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) || !state.getValue(CampfireBlock.LIT)) {
            return 0;
        }
        double radius = state.is(Blocks.SOUL_CAMPFIRE)
            ? com.protoano.granules.config.BalanceConfig.Setting.SOUL_CAMPFIRE_RADIUS.value()
            : com.protoano.granules.config.BalanceConfig.Setting.CAMPFIRE_RADIUS.value();
        return level.getBlockState(pos.below()).is(Blocks.HAY_BLOCK)
            ? radius * com.protoano.granules.config.BalanceConfig.Setting.HAY_RADIUS_MULTIPLIER.value() : radius;
    }

    public static Threat findThreat(Mob mob) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.CAMPFIRE_DETERRENCE)) {
            return null;
        }
        if (!(fearsCampfires(mob) || mob instanceof Creeper) || !(mob.level() instanceof ServerLevel level) || mob.isNoAi()) {
            return null;
        }
        Threat closest = null;
        double strongest = 0;
        int searchRadius = (int) Math.ceil(Math.max(com.protoano.granules.config.BalanceConfig.Setting.CREEPER_CAT_RADIUS.value(),
            Math.max(com.protoano.granules.config.BalanceConfig.Setting.CAMPFIRE_RADIUS.value(),
                com.protoano.granules.config.BalanceConfig.Setting.SOUL_CAMPFIRE_RADIUS.value())
                * com.protoano.granules.config.BalanceConfig.Setting.HAY_RADIUS_MULTIPLIER.value()));
        int minimumX = (mob.getBlockX() - searchRadius) >> 4;
        int maximumX = (mob.getBlockX() + searchRadius) >> 4;
        int minimumZ = (mob.getBlockZ() - searchRadius) >> 4;
        int maximumZ = (mob.getBlockZ() + searchRadius) >> 4;
        for (int x = minimumX; x <= maximumX; x++) {
            for (int z = minimumZ; z <= maximumZ; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    BlockState state = level.getBlockState(pos);
                    double radius = fearsCampfires(mob) ? campfireRadius(level, pos, state) : 0;
                    if (mob instanceof Creeper && blockEntity instanceof JukeboxBlockEntity jukebox
                        && jukebox.getSongPlayer().isPlaying()
                        && jukebox.getSongPlayer().getSong() == level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getOrThrow(JukeboxSongs.CAT).value()) {
                        radius = com.protoano.granules.config.BalanceConfig.Setting.CREEPER_CAT_RADIUS.value();
                    }
                    if (radius <= 0) {
                        continue;
                    }
                    Vec3 center = Vec3.atCenterOf(pos);
                    double distance = mob.position().distanceTo(center);
                    double strength = 1 - distance / radius;
                    if (strength >= strongest && distance <= radius) {
                        strongest = strength;
                        closest = new Threat(center, radius);
                    }
                }
            }
        }
        if (mob instanceof Creeper) {
            double ghastRadius = com.protoano.granules.config.BalanceConfig.Setting.CREEPER_GHAST_RADIUS.value();
            for (Mob ghast : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(ghastRadius),
                candidate -> candidate.isAlive() && (candidate instanceof Ghast || candidate.getType() == EntityTypes.HAPPY_GHAST))) {
                double distance = mob.distanceTo(ghast);
                double strength = ghastRadius > 0 ? 1 - distance / ghastRadius : -1;
                if (ghastRadius > 0 && distance <= ghastRadius && strength >= strongest) {
                    strongest = strength;
                    closest = new Threat(ghast.position(), ghastRadius);
                }
            }
        }
        return closest;
    }

    public static final class Controller {
        private Threat threat;
        private int scanTicks;
        private int pathTicks;
        private boolean fleeing;

        public boolean tick(Mob mob) {
            if (!(fearsCampfires(mob) || mob instanceof Creeper) || mob.isNoAi()) {
                return false;
            }
            if (scanTicks-- <= 0) {
                threat = findThreat(mob);
                scanTicks = 4;
            }
            if (threat == null || mob.position().distanceToSqr(threat.position) > threat.radius * threat.radius) {
                if (fleeing) {
                    mob.getNavigation().stop();
                    fleeing = false;
                }
                return false;
            }
            if (!fleeing) {
                mob.getNavigation().stop();
                pathTicks = 0;
                fleeing = true;
            }
            mob.setTarget(null);
            mob.setNoActionTime(mob.getNoActionTime() + 1);
            if (mob instanceof Creeper creeper && !creeper.isIgnited()) {
                creeper.setSwellDir(-1);
            }
            if (pathTicks-- <= 0 || mob.getNavigation().isDone()) {
                pathTicks = 10;
                moveAway(mob, threat.position);
            }
            mob.getNavigation().tick();
            mob.getMoveControl().tick();
            mob.getLookControl().tick();
            mob.getJumpControl().tick();
            return true;
        }

        private void moveAway(Mob mob, Vec3 source) {
            if (mob instanceof PathfinderMob pathfinder) {
                for (int attempt = 0; attempt < 6; attempt++) {
                    Vec3 destination = DefaultRandomPos.getPosAway(pathfinder, 16, 7, source);
                    if (destination == null || destination.distanceToSqr(source) <= mob.position().distanceToSqr(source)) {
                        continue;
                    }
                    if (mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.4)) {
                        return;
                    }
                }
            } else {
                Vec3 away = mob.position().subtract(source);
                if (away.lengthSqr() < 0.01) {
                    away = new Vec3(1, 0, 0);
                }
                Vec3 destination = mob.position().add(away.normalize().scale(12));
                if (mob instanceof Phantom) {
                    ((PhantomFlightAccessor) mob).granules$setFlightTarget(destination);
                }
                mob.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, 1.4);
            }
        }
    }

    public record Threat(Vec3 position, double radius) {
    }
}
