package com.protoano.granules.client;

import com.protoano.granules.config.ContentManifest;
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
            case ASPEN -> new String[] {"granules:aspen_sapling"};
            case CHORUS -> new String[] {"granules:chorus_block"};
            case TEA -> new String[] {"granules:tea_leaves"};
            case EXPLOSIVES -> new String[] {"granules:dynamite"};
            case GRAVE_SHOVEL -> new String[] {"granules:grave_shovel"};
            case UTILITY_TOOLS -> new String[] {"granules:iron_lock", "granules:gilded_tongs"};
            case PET_BEDS -> new String[] {"granules:pet_bed"};
            case STUMPS -> new String[] {"granules:oak_stump"};
            case CHISELED_LOGS -> new String[] {"granules:chiseled_oak_log"};
            case LITTER_CLUMPS -> new String[] {"granules:litter_clump"};
            case HERD_BEHAVIOR -> new String[] {"minecraft:cow_spawn_egg"};
            case CAMPFIRE_DETERRENCE -> new String[] {"minecraft:campfire"};
            case CRYSTAL_DEFENSE -> new String[] {"minecraft:end_crystal"};
            case TREASURE_SWORDS -> new String[] {"minecraft:enchanted_book"};
            case PLAYER_TITLES -> new String[] {"minecraft:name_tag"};
            case COPPER_GOLEM_UPGRADES -> new String[] {"minecraft:copper_golem_spawn_egg"};
		};
		int frame = (int) (Util.getMillis() / 1500L % ids.length);
		Identifier id = Identifier.parse(ids[frame]);
		return BuiltInRegistries.ITEM.get(id)
			.map(ItemStack::new)
			.orElse(ItemStack.EMPTY);
	}
}
