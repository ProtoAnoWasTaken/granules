package com.puppy.granules.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VoidFogPayload(boolean enabled) implements CustomPacketPayload {
	public static final Type<VoidFogPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("granules", "void_fog"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VoidFogPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL,
		VoidFogPayload::enabled,
		VoidFogPayload::new
	);

	@Override
	public Type<VoidFogPayload> type() {
		return TYPE;
	}
}
