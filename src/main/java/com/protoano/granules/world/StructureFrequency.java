package com.protoano.granules.world;

public final class StructureFrequency {
	private static volatile double multiplier = 1.0D;

	private StructureFrequency() {
	}

	public static double multiplier() {
		return multiplier;
	}

	public static void setMultiplier(double value) {
		multiplier = value;
	}
}
