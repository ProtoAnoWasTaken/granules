package com.protoano.granules.grave;

import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GraveGhostEntity extends Avatar {
    private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(GraveGhostEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ResolvableProfile> PROFILE = SynchedEntityData.defineId(GraveGhostEntity.class, EntityDataSerializers.RESOLVABLE_PROFILE);
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(GraveGhostEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(GraveGhostEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(GraveGhostEntity.class, EntityDataSerializers.BOOLEAN);
    public static EntityType.EntityFactory<GraveGhostEntity> clientFactory;
    public LivingEntity target;
    private UUID viewer;

    public GraveGhostEntity(EntityType<? extends GraveGhostEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setSilent(true);
        setGlowingTag(true);
        setInvisible(true);
    }

    public static GraveGhostEntity create(EntityType<GraveGhostEntity> type, Level level) {
        if (level.isClientSide() && clientFactory != null) {
            return clientFactory.create(type, level);
        }
        return new GraveGhostEntity(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VIEWER, "");
        builder.define(PROFILE, ResolvableProfile.Static.EMPTY);
        builder.define(MODEL, "minecraft:player");
        builder.define(BABY, false);
        builder.define(SITTING, false);
    }

    public String modelType() {
        return entityData.get(MODEL);
    }

    public boolean modelBaby() {
        return entityData.get(BABY);
    }

    public boolean modelSitting() {
        return entityData.get(SITTING);
    }

    @Override
    public ResolvableProfile getProfile() {
        return entityData.get(PROFILE);
    }

    public void configure(ServerPlayer owner, LivingEntity subject) {
        viewer = owner.getUUID();
        target = subject;
        entityData.set(MODEL, EntityType.getKey(subject.getType()).toString());
        entityData.set(BABY, subject.isBaby());
        entityData.set(VIEWER, viewer.toString());
        if (subject instanceof ServerPlayer player) {
            entityData.set(PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
        }
        setCustomName(subject.getDisplayName());
        setCustomNameVisible(true);
        follow(owner);
    }

    public boolean visibleTo(Player player) {
        return player != null && player.getUUID().toString().equals(entityData.get(VIEWER)) && GraveRescue.holdingShovel(player);
    }

    private void follow(ServerPlayer owner) {
        entityData.set(BABY, target.isBaby());
        entityData.set(SITTING, target instanceof net.minecraft.world.entity.TamableAnimal animal && animal.isInSittingPose());
        Vec3 projected = GraveRescue.animatedGhostPosition(target, (ServerLevel) level(), owner);
        snapTo(projected.x, projected.y, projected.z, target.getYRot(), target.getXRot());
        yHeadRot = target.getYHeadRot();
        yBodyRot = target.getYRot();
    }

    @Override
    public void tick() {
        tickCount++;
        if (level() instanceof ServerLevel level) {
            ServerPlayer owner = viewer == null ? null : level.getServer().getPlayerList().getPlayer(viewer);
            if (owner == null || target == null || !GraveRescue.canProject(owner, target)) {
                discard();
                return;
            }
            follow(owner);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player) {
            GraveRescue.contact(player, this, InteractionHand.MAIN_HAND);
        }
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!visibleTo(player) || !player.getItemInHand(hand).is(GraveContent.SHOVEL)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GraveRescue.contact(serverPlayer, this, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.Entity other) {
        return false;
    }
}
