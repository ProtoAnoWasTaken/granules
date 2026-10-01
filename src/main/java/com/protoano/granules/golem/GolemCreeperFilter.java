package com.protoano.granules.golem;

import com.protoano.granules.advancement.GranulesAdvancements;
import java.util.Comparator;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Items;

public final class GolemCreeperFilter {
    public static final AttachmentType<UUID> OWNER = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("granules", "golem_creeper_filter"),
        builder -> builder.persistent(UUIDUtil.CODEC));

    private GolemCreeperFilter() {
    }

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof IronGolem || entity instanceof SnowGolem)
                || !player.getItemInHand(hand).is(Items.GUNPOWDER) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                boolean removed = entity.getAttached(OWNER) != null;
                if (removed) {
                    entity.removeAttached(OWNER);
                    if (((Mob) entity).getTarget() instanceof Creeper) {
                        ((Mob) entity).setTarget(null);
                    }
                } else {
                    entity.setAttached(OWNER, player.getUUID());
                }
                player.getItemInHand(hand).consume(1, player);
                server.sendParticles(removed ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), 8, 0.4, 0.4, 0.4, 0.02);
            }
            return InteractionResult.SUCCESS;
        });
    }

    public static void tick(Mob golem) {
        if (!(golem instanceof IronGolem || golem instanceof SnowGolem) || golem.getAttached(OWNER) == null
            || !(golem.level() instanceof ServerLevel level) || golem.tickCount % 10 != 0) {
            return;
        }
        if (golem.getTarget() == null || !golem.getTarget().isAlive()) {
            var target = level.getEntitiesOfClass(Creeper.class, golem.getBoundingBox().inflate(32),
                candidate -> candidate.isAlive() && golem.getSensing().hasLineOfSight(candidate)).stream()
                .min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
            if (target != null) {
                golem.setTarget(target);
            }
        }
        recordAggression(golem);
    }

    public static void recordAggression(Mob golem) {
        UUID owner = golem.getAttached(OWNER);
        if (owner != null && golem.getTarget() instanceof Creeper && golem.level() instanceof ServerLevel level) {
            var player = level.getServer().getPlayerList().getPlayer(owner);
            if (player != null) {
                GranulesAdvancements.award(player, "less_friendly_fire");
            }
        }
    }
}
