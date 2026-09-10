package com.puppy.granules.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.puppy.granules.network.DiscPalettePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jspecify.annotations.Nullable;

public record UnmarkedDiscTint(int index) implements ItemTintSource {
    public static final MapCodec<UnmarkedDiscTint> CODEC = Codec.intRange(0, 2).fieldOf("index")
        .xmap(UnmarkedDiscTint::new, UnmarkedDiscTint::index);
    private static final int[] DEFAULT_COLORS = {0xFE3330, 0xAA2220, 0x6E1615};
    private static int[] blankColors = DEFAULT_COLORS;

    public static void initialize() {
        ItemTintSources.ID_MAPPER.put(Identifier.fromNamespaceAndPath("granules", "unmarked_disc"), CODEC);
        ClientPlayNetworking.registerGlobalReceiver(DiscPalettePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> blankColors = new int[] {payload.bright(), payload.medium(), payload.dark()});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> blankColors = DEFAULT_COLORS);
    }

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            if (data != null && data.getColor(index) != null) {
                return ARGB.opaque(data.getColor(index));
            }
        }
        return ARGB.opaque(blankColors[index]);
    }

    @Override
    public MapCodec<UnmarkedDiscTint> type() {
        return CODEC;
    }
}
