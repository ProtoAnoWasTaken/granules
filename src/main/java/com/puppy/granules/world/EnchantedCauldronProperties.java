package com.puppy.granules.world;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class EnchantedCauldronProperties {
	public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 9);

	private EnchantedCauldronProperties() {
	}

	public enum Content implements StringRepresentable {
		EMPTY("empty"),
		WATER("water"),
		HONEY("honey"),
		LAVA("lava"),
		POWDER_SNOW("powder_snow");

		private final String serializedName;

		Content(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return serializedName;
		}
	}
}
