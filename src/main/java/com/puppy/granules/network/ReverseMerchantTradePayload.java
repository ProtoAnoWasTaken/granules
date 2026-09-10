package com.puppy.granules.network;

import com.puppy.granules.GranulesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReverseMerchantTradePayload(int offerIndex) implements CustomPacketPayload {
	public static final Type<ReverseMerchantTradePayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "reverse_merchant_trade")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, ReverseMerchantTradePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		ReverseMerchantTradePayload::offerIndex,
		ReverseMerchantTradePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
