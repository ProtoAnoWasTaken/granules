package com.protoano.granules.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerHeadFilter {
    private PlayerHeadFilter() {
    }

    public static boolean resolve(ItemStack stack, Player user, Consumer<UUID> apply) {
        var profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile().id().equals(Util.NIL_UUID)) {
            return false;
        }
        if (!(user.level() instanceof ServerLevel level)) {
            return true;
        }
        UUID id = profile.partialProfile().id();
        String name = profile.name().orElse("");
        if (name.isEmpty() || !id.equals(UUIDUtil.createOfflinePlayerUUID(name))) {
            apply.accept(id);
            return true;
        }
        if (name.equalsIgnoreCase(user.getGameProfile().name())) {
            apply.accept(user.getUUID());
            return true;
        }
        var server = level.getServer();
        var online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            apply.accept(online.getUUID());
            return true;
        }
        net.minecraft.world.item.component.ResolvableProfile.createUnresolved(name)
            .resolveProfile(server.services().profileResolver())
            .thenAccept(resolved -> server.execute(() -> {
                if (!user.isRemoved() && !resolved.id().equals(Util.NIL_UUID)) {
                    apply.accept(resolved.id());
                }
            }));
        return true;
    }
}
