package com.puppy.granules.client;

import com.puppy.granules.compat.KillerRabbitJadeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class KillerRabbitJadeComponentProvider implements IEntityComponentProvider {
    public static final KillerRabbitJadeComponentProvider INSTANCE = new KillerRabbitJadeComponentProvider();

    private KillerRabbitJadeComponentProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag status = accessor.getServerData().getCompoundOrEmpty(KillerRabbitJadeProvider.DATA_KEY);
        if (status.isEmpty()) {
            return;
        }
        boolean tamed = status.getBooleanOr("Tamed", false);
        String owner = status.getStringOr("OwnerName", status.getStringOr("Owner", ""));
        Component ownerName = owner.isEmpty()
            ? Component.translatable("jade.granules.killer_rabbit.untamed")
            : Component.literal(owner);
        tooltip.add(
            Component.translatable("jade.granules.killer_rabbit.master")
                .withStyle(ChatFormatting.GRAY)
                .append(ownerName.copy().withStyle(ChatFormatting.WHITE))
        );
        if (tamed) {
            String statusKey = status.getBooleanOr("Sitting", false)
                ? "jade.granules.killer_rabbit.sitting"
                : "jade.granules.killer_rabbit.tamed";
            tooltip.add(Component.translatable(statusKey));
            return;
        }
        int progress = status.getIntOr("Progress", 0);
        int target = status.getIntOr("Target", 0);
        Object displayedTarget = target == 0 ? "?" : target;
        tooltip.add(Component.translatable("jade.granules.killer_rabbit.hunger", progress, displayedTarget));
        int cooldown = status.getIntOr("Cooldown", 0);
        Component cooldownLine = cooldown > 0
            ? Component.translatable("jade.granules.killer_rabbit.cooldown", (cooldown + 19) / 20)
            : Component.translatable("jade.granules.killer_rabbit.ready");
        tooltip.add(cooldownLine);
    }

    @Override
    public Identifier getUid() {
        return KillerRabbitJadeProvider.UID;
    }
}
