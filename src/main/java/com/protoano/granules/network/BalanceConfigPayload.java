package com.protoano.granules.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BalanceConfigPayload(String json) implements CustomPacketPayload {
    public static final Type<BalanceConfigPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("granules", "balance_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BalanceConfigPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, BalanceConfigPayload::json, BalanceConfigPayload::new);

    @Override
    public Type<BalanceConfigPayload> type() {
        return TYPE;
    }
}
