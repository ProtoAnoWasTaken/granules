package com.puppy.granules.world;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class SealedBarrelProperties {
	public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
	public static final String DATA_KEY = "granules_sealed";
	public static final Identifier ITEM_MODEL = Identifier.fromNamespaceAndPath("granules", "sealed_barrel");

	private SealedBarrelProperties() {
	}
}
