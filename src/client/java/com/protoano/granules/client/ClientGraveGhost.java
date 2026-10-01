package com.protoano.granules.client;

import com.protoano.granules.grave.GraveGhostEntity;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;

public final class ClientGraveGhost extends GraveGhostEntity implements ClientAvatarEntity {
    private final ClientAvatarState avatar = new ClientAvatarState();
    private ResolvableProfile cachedProfile;
    private Supplier<PlayerSkin> skin;
    private net.minecraft.world.entity.LivingEntity model;
    private String modelId;

    public net.minecraft.world.entity.LivingEntity modelEntity() {
        if (modelType().equals("minecraft:player")) {
            return this;
        }
        if (!modelType().equals(modelId)) {
            modelId = modelType();
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse(modelId));
            var entity = type == null ? null : type.create(level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            model = entity instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
        }
        if (model == null) {
            return this;
        }
        model.setId(getId());
        model.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        model.yHeadRot = yHeadRot;
        model.yHeadRotO = yHeadRotO;
        model.yBodyRot = yBodyRot;
        model.yBodyRotO = yBodyRotO;
        model.tickCount = tickCount;
        model.setInvisible(true);
        model.setGlowingTag(true);
        if (model instanceof net.minecraft.world.entity.AgeableMob ageable) {
            ageable.setBaby(modelBaby());
        }
        if (model instanceof net.minecraft.world.entity.TamableAnimal animal) {
            animal.setInSittingPose(modelSitting());
        }
        return model;
    }

    public ClientGraveGhost(EntityType<GraveGhostEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public ClientAvatarState avatarState() {
        return avatar;
    }

    @Override
    public void tick() {
        super.tick();
        getInterpolation().interpolate();
        yBodyRotO = yBodyRot;
        yHeadRotO = yHeadRot;
        yBodyRot = getYRot();
        if (lerpHeadSteps > 0) {
            yHeadRot += net.minecraft.util.Mth.wrapDegrees((float) lerpYHeadRot - yHeadRot) / lerpHeadSteps;
            lerpHeadSteps--;
        }
        avatar.tick(position(), getDeltaMovement());
    }

    @Override
    public boolean isPickable() {
        return visibleTo(Minecraft.getInstance().player);
    }

    @Override
    public PlayerSkin getSkin() {
        if (!getProfile().equals(cachedProfile)) {
            cachedProfile = getProfile();
            skin = Minecraft.getInstance().getSkinManager().createLookup(cachedProfile.partialProfile(), false);
        }
        return skin.get();
    }

    @Override
    public Parrot.Variant getParrotVariantOnShoulder(boolean left) {
        return null;
    }

    @Override
    public boolean showExtraEars() {
        return false;
    }
}
