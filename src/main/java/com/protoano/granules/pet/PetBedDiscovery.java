package com.protoano.granules.pet;

import com.protoano.granules.mixin.GraveEntityStorageAccessor;
import com.protoano.granules.mixin.GraveLevelEntitiesAccessor;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PetBedDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger("Granules Pet Beds");
    private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE,
        Identifier.fromNamespaceAndPath("granules", "pet_bed_summon"),
        new TicketType(40, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    private static final Map<UUID, StoredPet> PETS = new HashMap<>();
    private static final Set<UUID> OBSERVED = new HashSet<>();
    private static final ArrayDeque<SearchChunk> QUEUE = new ArrayDeque<>();
    private static final Map<UUID, PendingAction> ACTIONS = new HashMap<>();
    private static MinecraftServer activeServer;
    private static boolean started;
    private static int pending;

    private PetBedDiscovery() {
    }

    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> observe(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> observe(entity));
        ServerTickEvents.END_SERVER_TICK.register(PetBedDiscovery::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
    }

    public static void clear() {
        PETS.clear();
        OBSERVED.clear();
        QUEUE.clear();
        ACTIONS.clear();
        activeServer = null;
        started = false;
        pending = 0;
    }

    public static void observe(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity instanceof ServerPlayer) {
            return;
        }
        UUID id = entity.getUUID();
        UUID owner = PetBedService.ownerOf(living);
        if (owner == null && !living.hasCustomName()) {
            PETS.remove(id);
            return;
        }
        OBSERVED.add(id);
        if (living.getHealth() <= 0 || entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
            PETS.remove(id);
            return;
        }
        PETS.put(id, new StoredPet((ServerLevel) living.level(), living.chunkPosition(), owner,
            Set.copyOf(living.entityTags()), PetBedService.entryFor(living)));
    }

    public static List<PetBedService.PetEntry> entriesFor(ServerPlayer player) {
        start(player.level().getServer());
        List<PetBedService.PetEntry> entries = new ArrayList<>();
        for (StoredPet pet : PETS.values()) {
            if ((pet.owner == null || pet.owner.equals(player.getUUID()))
                && !pet.tags.contains(PetBedService.delistedTag(player.getUUID()))) {
                entries.add(pet.entry);
            }
        }
        return entries;
    }

    public static void loadForAction(ServerPlayer player, UUID petId, Consumer<ServerPlayer> action) {
        StoredPet pet = PETS.get(petId);
        if (pet == null || pet.owner != null && !pet.owner.equals(player.getUUID())
            || pet.tags.contains(PetBedService.delistedTag(player.getUUID()))) {
            return;
        }
        ACTIONS.put(player.getUUID(), new PendingAction(petId, player.containerMenu,
            player.level().getServer().getTickCount() + 200, action));
        pet.level.getChunkSource().addTicketWithRadius(TICKET, pet.chunk, 2);
    }

    private static void start(MinecraftServer server) {
        activeServer = server;
        if (started) {
            return;
        }
        started = true;
        List<SearchFolder> folders = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            Path folder = DimensionType.getStorageFolder(level.dimension(), server.getWorldPath(LevelResource.ROOT)).resolve("entities");
            folders.add(new SearchFolder(level, folder));
        }
        CompletableFuture.supplyAsync(() -> savedChunks(folders)).whenComplete((chunks, failure) -> server.execute(() -> {
            if (activeServer != server) {
                return;
            }
            if (failure != null) {
                LOGGER.warn("Could not enumerate saved pets", failure);
                started = false;
                return;
            }
            QUEUE.addAll(chunks);
        }));
    }

    private static List<SearchChunk> savedChunks(List<SearchFolder> folders) {
        List<SearchChunk> chunks = new ArrayList<>();
        for (SearchFolder folder : folders) {
            if (!Files.isDirectory(folder.path)) {
                continue;
            }
            try (var paths = Files.list(folder.path)) {
                for (Path path : paths.filter(file -> file.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca")).toList()) {
                    String[] parts = path.getFileName().toString().split("\\.");
                    int regionX = Integer.parseInt(parts[1]);
                    int regionZ = Integer.parseInt(parts[2]);
                    try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
                        byte[] header = input.readNBytes(4096);
                        if (header.length < 4096) {
                            continue;
                        }
                        ByteBuffer offsets = ByteBuffer.wrap(header);
                        for (int index = 0; index < 1024; index++) {
                            if (offsets.getInt() != 0) {
                                chunks.add(new SearchChunk(folder.level, new ChunkPos(regionX * 32 + index % 32, regionZ * 32 + index / 32)));
                            }
                        }
                    }
                }
            } catch (IOException exception) {
                LOGGER.warn("Could not scan pet entity storage at {}", folder.path, exception);
            }
        }
        return chunks;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PendingAction action = ACTIONS.get(player.getUUID());
            if (action == null) {
                continue;
            }
            if (player.containerMenu != action.menu || !action.menu.stillValid(player) || server.getTickCount() > action.expires) {
                ACTIONS.remove(player.getUUID());
                continue;
            }
            StoredPet pet = PETS.get(action.pet);
            if (pet == null) {
                ACTIONS.remove(player.getUUID());
                continue;
            }
            pet.level.getChunkSource().addTicketWithRadius(TICKET, pet.chunk, 2);
            if (PetBedService.findLivePet(player, action.pet) != null) {
                ACTIONS.remove(player.getUUID());
                action.action.accept(player);
            }
        }
        ACTIONS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        boolean menuOpen = server.getPlayerList().getPlayers().stream().anyMatch(player -> player.containerMenu instanceof PetBedMenu);
        if (!menuOpen || server.getTickCount() % 5 != 0) {
            return;
        }
        for (int count = 0; count < 16 && pending < 32 && !QUEUE.isEmpty(); count++) {
            SearchChunk search = QUEUE.removeFirst();
            pending++;
            var manager = ((GraveLevelEntitiesAccessor) search.level).granules$entityManager();
            var storage = ((GraveEntityStorageAccessor) manager).granules$storage();
            storage.loadEntities(search.chunk).whenComplete((stored, failure) -> server.execute(() -> {
                if (activeServer != server) {
                    return;
                }
                pending--;
                if (failure != null) {
                    LOGGER.warn("Could not read pets in {} {}", search.level.dimension(), search.chunk, failure);
                    return;
                }
                stored.getEntities().forEach(entity -> {
                    if (!OBSERVED.contains(entity.getUUID())) {
                        observe(entity);
                    }
                });
            }));
        }
    }

    private record StoredPet(ServerLevel level, ChunkPos chunk, UUID owner, Set<String> tags, PetBedService.PetEntry entry) {
    }

    private record SearchFolder(ServerLevel level, Path path) {
    }

    private record SearchChunk(ServerLevel level, ChunkPos chunk) {
    }

    private record PendingAction(UUID pet, AbstractContainerMenu menu, int expires, Consumer<ServerPlayer> action) {
    }
}
