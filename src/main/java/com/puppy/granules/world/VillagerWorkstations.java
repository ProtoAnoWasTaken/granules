package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.mixin.access.PoiTypesAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class VillagerWorkstations {
	private VillagerWorkstations() {
	}

	public static void initialize() {
		registerEquivalentWorkstation(GranulesMod.ENCHANTED_SMITHING_TABLE, PoiTypes.TOOLSMITH);
		registerEquivalentWorkstation(GranulesMod.ENCHANTED_GRINDSTONE, PoiTypes.WEAPONSMITH);
		registerEquivalentWorkstation(GranulesMod.ENCHANTED_STONECUTTER, PoiTypes.MASON);
		registerEquivalentWorkstation(GranulesMod.ENCHANTED_FLETCHING_TABLE, PoiTypes.FLETCHER);
	}

	private static void registerEquivalentWorkstation(Block block, net.minecraft.resources.ResourceKey<PoiType> poiKey) {
		Holder<PoiType> poi = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(poiKey);
		for (BlockState state : block.getStateDefinition().getPossibleStates()) {
			PoiTypesAccess.granules$getTypeByState().put(state, poi);
		}
	}
}
