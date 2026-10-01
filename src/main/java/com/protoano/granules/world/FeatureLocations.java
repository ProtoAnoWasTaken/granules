package com.protoano.granules.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class FeatureLocations extends SavedData {
    public static final Codec<FeatureLocations> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG_STREAM)
        .xmap(FeatureLocations::new, FeatureLocations::snapshot);
    private static final SavedDataType<FeatureLocations> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("granules", "feature_locations"), FeatureLocations::new, CODEC, null);
    private final Map<Identifier, LongOpenHashSet> positions = new HashMap<>();

    public FeatureLocations() {
    }

    private FeatureLocations(Map<Identifier, LongStream> saved) {
        saved.forEach((feature, entries) -> positions.put(feature, new LongOpenHashSet(entries.toArray())));
    }

    public static FeatureLocations get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void record(Identifier feature, BlockPos position) {
        if (positions.computeIfAbsent(feature, key -> new LongOpenHashSet()).add(position.asLong())) {
            setDirty();
        }
    }

    public Optional<BlockPos> nearest(Identifier feature, BlockPos origin, int radius) {
        LongOpenHashSet entries = positions.get(feature);
        if (entries == null) {
            return Optional.empty();
        }
        BlockPos nearest = null;
        double best = (double) radius * radius;
        for (long packed : entries) {
            BlockPos position = BlockPos.of(packed);
            double distance = horizontalDistanceSquared(origin, position);
            if (distance < best || distance == best && (nearest == null || position.compareTo(nearest) < 0)) {
                best = distance;
                nearest = position;
            }
        }
        return Optional.ofNullable(nearest);
    }

    public static double horizontalDistanceSquared(BlockPos first, BlockPos second) {
        double x = (double) first.getX() - second.getX();
        double z = (double) first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private Map<Identifier, LongStream> snapshot() {
        Map<Identifier, LongStream> result = new HashMap<>();
        positions.forEach((feature, entries) -> result.put(feature, LongStream.of(entries.toLongArray())));
        return result;
    }
}
