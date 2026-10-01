package com.protoano.granules.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class FeatureLocator {
    private static final Map<ServerLevel, ConcurrentLinkedQueue<Placement>> PENDING = new ConcurrentHashMap<>();
    public static final Identifier DESERT_WELL = Identifier.withDefaultNamespace("desert_well");
    public static final Identifier BADLANDS_WELL = Identifier.fromNamespaceAndPath("granules", "badlands_well");
    public static final Identifier SHORE_ARCHAEOLOGY = Identifier.fromNamespaceAndPath("granules", "shore_archaeology");

    private FeatureLocator() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(FeatureLocator::flush);
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            scanWells(level, chunk);
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> {
            flush(level);
            PENDING.remove(level);
        });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            for (ServerLevel level : server.getAllLevels()) {
                flush(level);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING.keySet().removeIf(level -> level.getServer() == server);
        });
    }

    public static void record(WorldGenLevel world, ConfiguredFeature<?, ?> feature, BlockPos position) {
        Identifier id = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getKey(feature);
        if (id != null && !id.equals(SHORE_ARCHAEOLOGY)) {
            record(world, id, position);
        }
    }

    public static void record(WorldGenLevel world, Identifier feature, BlockPos position) {
        PENDING.computeIfAbsent(world.getLevel(), level -> new ConcurrentLinkedQueue<>())
            .add(new Placement(feature, position.immutable()));
    }

    public static void flush(ServerLevel level) {
        ConcurrentLinkedQueue<Placement> queue = PENDING.get(level);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        FeatureLocations locations = FeatureLocations.get(level);
        Placement placement;
        while ((placement = queue.poll()) != null) {
            locations.record(placement.feature(), placement.position());
        }
    }

    public static void scanNearbyWells(ServerLevel level, BlockPos origin, int radius) {
        int chunkRadius = Math.min(radius, 256) / 16 + 1;
        int centerX = origin.getX() >> 4;
        int centerZ = origin.getZ() >> 4;
        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
            for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk != null) {
                    scanWells(level, chunk);
                }
            }
        }
    }

    public static void scanWells(ServerLevel level, LevelChunk chunk) {
        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int roofY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
                BlockPos roof = new BlockPos(x, roofY, z);
                Block roofBlock = chunk.getBlockState(roof).getBlock();
                boolean red = roofBlock == Blocks.RED_SANDSTONE;
                if (!red && roofBlock != Blocks.SANDSTONE) {
                    continue;
                }
                BlockPos center = roof.below(4);
                if (isWell(level, center, red)) {
                    FeatureLocations.get(level).record(red ? BADLANDS_WELL : DESERT_WELL, center);
                }
            }
        }
    }

    public static boolean isWell(ServerLevel level, BlockPos center, boolean red) {
        Block stone = red ? Blocks.RED_SANDSTONE : Blocks.SANDSTONE;
        Block slab = red ? Blocks.RED_SANDSTONE_SLAB : Blocks.SANDSTONE_SLAB;
        if (!isBlock(level, center, Blocks.WATER) || !isBlock(level, center.above(4), stone)) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!isBlock(level, center.relative(direction), Blocks.WATER)
                || !isBlock(level, center.relative(direction).above(4), slab)
                || !isBlock(level, center.relative(direction, 2).above(), slab)) {
                return false;
            }
        }
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                for (int y = 1; y <= 3; y++) {
                    if (!isBlock(level, center.offset(x, y, z), stone)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isBlock(ServerLevel level, BlockPos position, Block block) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        return chunk != null && chunk.getBlockState(position).is(block);
    }

    private record Placement(Identifier feature, BlockPos position) {
    }
}
