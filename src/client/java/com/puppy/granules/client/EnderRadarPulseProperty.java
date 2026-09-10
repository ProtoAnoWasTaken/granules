package com.puppy.granules.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class EnderRadarPulseProperty implements RangeSelectItemModelProperty {
	public static final MapCodec<EnderRadarPulseProperty> MAP_CODEC = MapCodec.unit(new EnderRadarPulseProperty());

	@Override
	public float get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		if (level == null) {
			return 0.0F;
		}
		return level.getGameTime() % 20L < 10L ? 0.0F : 1.0F;
	}

	@Override
	public MapCodec<EnderRadarPulseProperty> type() {
		return MAP_CODEC;
	}
}
