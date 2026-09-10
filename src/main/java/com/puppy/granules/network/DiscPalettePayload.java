package com.puppy.granules.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DiscPalettePayload(int bright, int medium, int dark) implements CustomPacketPayload {
    public static final Type<DiscPalettePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("granules", "disc_palette"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DiscPalettePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        DiscPalettePayload::bright,
        ByteBufCodecs.INT,
        DiscPalettePayload::medium,
        ByteBufCodecs.INT,
        DiscPalettePayload::dark,
        DiscPalettePayload::new
    );

    @Override
    public Type<DiscPalettePayload> type() {
        return TYPE;
    }
}
