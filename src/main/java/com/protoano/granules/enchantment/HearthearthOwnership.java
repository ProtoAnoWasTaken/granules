package com.protoano.granules.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class HearthearthOwnership extends SavedData {
    public static final Codec<HearthearthOwnership> CODEC = Claim.CODEC.listOf()
        .xmap(HearthearthOwnership::new, data -> List.copyOf(data.claims));
    private static final SavedDataType<HearthearthOwnership> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("granules", "hearthearth_ownership"), HearthearthOwnership::new, CODEC, null);
    private final List<Claim> claims = new ArrayList<>();

    private record Claim(UUID owner, GlobalPos location) {
        private static final Codec<Claim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Claim::owner),
            GlobalPos.CODEC.fieldOf("location").forGetter(Claim::location)
        ).apply(instance, Claim::new));
    }

    public HearthearthOwnership() {
    }

    private HearthearthOwnership(List<Claim> saved) {
        claims.addAll(saved);
    }

    public static HearthearthOwnership get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean hasSword(UUID owner) {
        return claims.stream().anyMatch(claim -> claim.owner().equals(owner));
    }

    public void record(CampfireBlockEntity campfire, UUID owner) {
        if (campfire.getLevel() instanceof ServerLevel level) {
            var claim = new Claim(owner, GlobalPos.of(level.dimension(), campfire.getBlockPos()));
            if (!claims.contains(claim)) {
                claims.add(claim);
                setDirty();
            }
        }
    }

    public void release(CampfireBlockEntity campfire, UUID owner) {
        if (campfire.getLevel() instanceof ServerLevel level
                && claims.remove(new Claim(owner, GlobalPos.of(level.dimension(), campfire.getBlockPos())))) {
            setDirty();
        }
    }

    public boolean hasSword(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!hasSword(player.getUUID()) && player instanceof ServerPlayer serverPlayer) {
            var respawn = serverPlayer.getRespawnConfig();
            if (respawn != null) {
                var target = level.getServer().getLevel(respawn.respawnData().dimension());
                if (target != null) {
                    var pos = respawn.respawnData().pos();
                    target.getChunkAt(pos);
                    if (target.getBlockEntity(pos) instanceof CampfireBlockEntity campfire
                            && HearthearthCampfire.get(campfire).ownsSword(player.getUUID())) {
                        record(campfire, player.getUUID());
                    }
                }
            }
        }
        boolean removed = claims.removeIf(claim -> {
            if (!claim.owner().equals(player.getUUID())) {
                return false;
            }
            var target = level.getServer().getLevel(claim.location().dimension());
            if (target == null || !target.hasChunkAt(claim.location().pos())) {
                return false;
            }
            return !(target.getBlockEntity(claim.location().pos()) instanceof CampfireBlockEntity campfire)
                || !HearthearthCampfire.get(campfire).ownsSword(player.getUUID());
        });
        if (removed) {
            setDirty();
        }
        return hasSword(player.getUUID());
    }
}
