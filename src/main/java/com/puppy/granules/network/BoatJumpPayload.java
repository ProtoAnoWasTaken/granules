package com.puppy.granules.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BoatJumpPayload(int boatId, float charge) implements CustomPacketPayload {
	public static final Type<BoatJumpPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("granules", "boat_jump"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BoatJumpPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		BoatJumpPayload::boatId,
		ByteBufCodecs.FLOAT,
		BoatJumpPayload::charge,
		BoatJumpPayload::new
	);

	@Override
	public Type<BoatJumpPayload> type() {
		return TYPE;
	}
}
