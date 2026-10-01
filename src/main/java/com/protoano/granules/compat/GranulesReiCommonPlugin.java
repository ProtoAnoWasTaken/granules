package com.protoano.granules.compat;

import com.protoano.granules.GranulesMod;
import com.protoano.granules.fletching.FletchingRecipe;
import com.protoano.granules.fletching.ArrowParts;
import com.protoano.granules.config.ContentManifest;
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
		if (ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
			return;
		}
		registry.beginRecipeFiller(FletchingRecipe.class)
			.filterType(GranulesMod.FLETCHING_RECIPE_TYPE)
			.fill(FletchingDisplay::new);
		for (ArrowParts parts : ArrowParts.ALL) {
			if (!parts.isBasic()) {
				registry.add(new FletchingDisplay(parts));
			}
		}
	}
}
