package com.puppy.granules.block;

import net.minecraft.util.StringRepresentable;

public enum PlanterSoil implements StringRepresentable {
	EMPTY("empty"),
	DIRT("dirt"),
	MUD("mud"),
	FARMLAND("farmland"),
	WET_FARMLAND("wet_farmland"),
	GRASS("grass"),
	PODZOL("podzol"),
	SAND("sand"),
	NETHERRACK("netherrack"),
	SOUL_SAND("soul_sand"),
	SOUL_SOIL("soul_soil");

	private final String serializedName;

	PlanterSoil(String serializedName) {
		this.serializedName = serializedName;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
