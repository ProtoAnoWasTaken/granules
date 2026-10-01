package com.protoano.granules.grave;

import com.protoano.granules.mixin.GraveEntityStorageAccessor;
import com.protoano.granules.mixin.GraveLevelEntitiesAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;

public final class GraveDiscovery {
    private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE,
        Identifier.fromNamespaceAndPath("granules", "grave_search"),
        new TicketType(60, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    private static final Map<Search, Long> SEARCHED = new HashMap<>();
    private static MinecraftServer activeServer;
    private static int pending;

    private GraveDiscovery() {
    }

    public static void initialize() {
    }

    public static void clear() {
        SEARCHED.clear();
        activeServer = null;
        pending = 0;
    }

    public static void tick(MinecraftServer server) {
        activeServer = server;
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        long now = server.getTickCount();
        SEARCHED.entrySet().removeIf(entry -> {
            ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey().viewer);
            return now - entry.getValue() > 6000 || viewer == null || !GraveRescue.holdingShovel(viewer);
        });
        int budget = 32;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GraveRescue.holdingShovel(player)) {
                continue;
            }
            for (ServerLevel level : server.getAllLevels()) {
                if (level == player.level()) {
                    continue;
                }
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof LivingEntity living && !(living instanceof ServerPlayer) && GraveRescue.canProject(player, living)) {
                        keepLoaded(level, living.chunkPosition());
                    }
                }
                double scale = DimensionType.getTeleportationScale(player.level().dimensionType(), level.dimensionType());
                int centerX = (int) Math.floor(player.getX() * scale / 16);
                int centerZ = (int) Math.floor(player.getZ() * scale / 16);
                int radius = (int) Math.ceil(com.protoano.granules.config.BalanceConfig.Setting.GRAVE_GHOST_RADIUS.value() * scale / 16) + 1;
                for (int ring = 0; ring <= radius; ring++) {
                    for (int x = -ring; x <= ring; x++) {
                        for (int z = -ring; z <= ring; z++) {
                            if (Math.max(Math.abs(x), Math.abs(z)) != ring || pending >= 64 || budget <= 0) {
                                continue;
                            }
                            ChunkPos chunk = new ChunkPos(centerX + x, centerZ + z);
                            Search search = new Search(player.getUUID(), level, chunk);
                            if (SEARCHED.containsKey(search)) {
                                continue;
                            }
                            SEARCHED.put(search, now);
                            budget--;
                            pending++;
                            var manager = ((GraveLevelEntitiesAccessor) level).granules$entityManager();
                            var storage = ((GraveEntityStorageAccessor) manager).granules$storage();
                            storage.loadEntities(chunk).whenComplete((stored, failure) -> server.execute(() -> {
                                if (activeServer != server) {
                                    return;
                                }
                                pending--;
                                ServerPlayer viewer = server.getPlayerList().getPlayer(search.viewer);
                                if (failure == null && viewer != null && GraveRescue.holdingShovel(viewer)
                                    && stored.getEntities().anyMatch(entity -> entity instanceof LivingEntity living && GraveRescue.canProject(viewer, living))) {
                                    keepLoaded(level, chunk);
                                }
                            }));
                        }
                    }
                }
            }
        }
    }

    private static void keepLoaded(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addTicketWithRadius(TICKET, chunk, 2);
    }

    private record Search(UUID viewer, ServerLevel level, ChunkPos chunk) {
    }
}
