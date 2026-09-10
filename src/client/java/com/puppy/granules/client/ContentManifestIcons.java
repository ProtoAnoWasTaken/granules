package com.puppy.granules.client;

import com.puppy.granules.config.ContentManifest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

public final class ContentManifestIcons {
	private ContentManifestIcons() {
	}

	public static ItemStack icon(ContentManifest.Category category) {
		String[] ids = switch (category) {
			case ENHANCED_WORKSTATIONS -> new String[] {
				"granules:enchanted_tome",
				"granules:planter",
				"minecraft:fletching_table"
			};
			case UNMARKED_DISCS -> new String[] {
				"granules:unmarked_disc"
			};
			case THE_BURROW -> new String[] {
				"granules:rabbit_hole",
				"granules:killer_bunny_spawn_egg"
			};
			case OLD_WORLD_ITEMS -> new String[] {
				"granules:old_world_cod",
				"granules:old_world_rose",
				"granules:ender_radar"
			};
			case GLOW_WOOL -> new String[] {
				"minecraft:glow_ink_sac"
			};
			case MOVERS -> new String[] {
				"granules:mover"
			};
			case LESSER_ITEMS -> new String[] {
				"granules:ender_pearlet",
				"granules:phantom_scale",
				"granules:bread_crumbs"
			};
		};
		int frame = (int) (Util.getMillis() / 1500L % ids.length);
		Identifier id = Identifier.parse(ids[frame]);
		return BuiltInRegistries.ITEM.get(id)
			.map(ItemStack::new)
			.orElse(ItemStack.EMPTY);
	}
}
