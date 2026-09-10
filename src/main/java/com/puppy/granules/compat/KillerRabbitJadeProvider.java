package com.puppy.granules.compat;

import com.puppy.granules.rabbit.KillerRabbitAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.UUID;

public final class KillerRabbitJadeProvider implements IServerDataProvider<EntityAccessor> {
    public static final KillerRabbitJadeProvider INSTANCE = new KillerRabbitJadeProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath("granules", "killer_rabbit");
    public static final String DATA_KEY = "granules:killer_rabbit";

    private KillerRabbitJadeProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof Rabbit rabbit) || rabbit.getVariant() != Rabbit.Variant.EVIL) {
            return;
        }
        KillerRabbitAccess rabbitData = (KillerRabbitAccess) rabbit;
        CompoundTag status = new CompoundTag();
        UUID ownerUuid = rabbitData.granules$ownerUuid();
        if (ownerUuid != null) {
            status.putString("Owner", ownerUuid.toString());
            if (accessor.getLevel() instanceof ServerLevel level) {
                Entity owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
                if (owner != null) {
                    status.putString("OwnerName", owner.getName().getString());
                }
            }
        }
        status.putInt("Progress", rabbitData.granules$feedingProgress());
        status.putInt("Target", rabbitData.granules$feedingTarget());
        status.putInt("Cooldown", rabbitData.granules$feedingCooldown());
        status.putBoolean("Tamed", rabbitData.granules$isTamedKillerRabbit());
        status.putBoolean("Sitting", rabbitData.granules$isOrderedToSit());
        data.put(DATA_KEY, status);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
