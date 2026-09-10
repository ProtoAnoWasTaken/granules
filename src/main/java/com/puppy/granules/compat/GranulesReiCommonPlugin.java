package com.puppy.granules.compat;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.fletching.FletchingRecipe;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import net.minecraft.resources.Identifier;

public final class GranulesReiCommonPlugin implements REICommonPlugin {
	@Override
	public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
		registry.register(Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "fletching"), FletchingDisplay.SERIALIZER);
	}

	@Override
	public void registerDisplays(ServerDisplayRegistry registry) {
		registry.beginRecipeFiller(FletchingRecipe.class)
			.filterType(GranulesMod.FLETCHING_RECIPE_TYPE)
			.fill(FletchingDisplay::new);
	}
}
