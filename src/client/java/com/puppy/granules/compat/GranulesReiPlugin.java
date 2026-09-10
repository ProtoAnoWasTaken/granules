package com.puppy.granules.compat;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;
import com.puppy.granules.rabbit.PalePeltEquipment;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;

public final class GranulesReiPlugin implements REIClientPlugin {
	public static final CategoryIdentifier<FletchingDisplay> FLETCHING = FletchingDisplay.CATEGORY;

	@Override
	public void registerCategories(CategoryRegistry registry) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
			return;
		}
		registry.add(new FletchingDisplayCategory());
		registry.addWorkstations(FLETCHING, EntryStacks.of(net.minecraft.world.level.block.Blocks.FLETCHING_TABLE));
		registry.addWorkstations(FLETCHING, EntryStacks.of(GranulesMod.ENCHANTED_FLETCHING_TABLE));
	}

	@Override
	public void registerEntries(EntryRegistry registry) {
		if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)) {
			moveAfter(registry, Items.TURTLE_HELMET, GranulesMod.PHANTOM_SCALE);
			moveAfter(registry, Items.NETHER_STAR, GranulesMod.ENDER_PEARLET);
			moveAfter(registry, Items.BREAD, GranulesMod.BREAD_HEELS);
			moveAfter(registry, GranulesMod.BREAD_HEELS, GranulesMod.BREAD_CRUMBS);
			moveAfter(registry, Items.DYED_BUNDLE.black(), GranulesMod.SOMNOSATCHEL);
		}
		moveAfter(registry, Items.POWDER_SNOW_BUCKET, GranulesMod.HONEY_BUCKET);
		moveAfter(registry, Items.COMPARATOR, GranulesMod.REDSTONE_RESISTOR_ITEM);
		if (!ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
			moveAfter(registry, Items.LODESTONE, GranulesMod.PHILOSOPHER_ITEM);
			moveAfter(registry, Items.OBSERVER, GranulesMod.CASTER_ITEM);
			moveAfter(registry, Items.ARROW, GranulesMod.FLETCHERS_ARROW);
			moveAfter(registry, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, GranulesMod.ENCHANTED_TOME);
			moveAfter(registry, Items.ANVIL, GranulesMod.ENCHANTED_ANVIL_ITEM);
			moveAfter(registry, Items.ENCHANTING_TABLE, GranulesMod.ENCHANTED_ENCHANTING_TABLE_ITEM);
			moveAfter(registry, Items.SMITHING_TABLE, GranulesMod.ENCHANTED_SMITHING_TABLE_ITEM);
			moveAfter(registry, Items.CRAFTING_TABLE, GranulesMod.ENCHANTED_CRAFTING_TABLE_ITEM);
			moveAfter(registry, Items.FLETCHING_TABLE, GranulesMod.ENCHANTED_FLETCHING_TABLE_ITEM);
			moveAfter(registry, Items.STONECUTTER, GranulesMod.ENCHANTED_STONECUTTER_ITEM);
			moveAfter(registry, Items.GRINDSTONE, GranulesMod.ENCHANTED_GRINDSTONE_ITEM);
			moveAfter(registry, Items.CAULDRON, GranulesMod.ENCHANTED_CAULDRON_ITEM);
		}
		if (!ContentManifest.get().isBanned(ContentManifest.Category.MOVERS)) {
			moveAfter(registry, GranulesMod.CASTER_ITEM, GranulesMod.MOVER_ITEM);
			moveAfter(registry, GranulesMod.MOVER_ITEM, GranulesMod.JUNCTION_ITEM);
		}
		if (!ContentManifest.get().isBanned(ContentManifest.Category.GLOW_WOOL)) {
			ItemLike glowWoolAnchor = GranulesMod.getVanillaWoolBlock(DyeColor.BLACK);
			for (DyeColor color : DyeColor.values()) {
				ItemLike glowWoolItem = GranulesMod.getGlowWoolItem(color);
				moveAfter(registry, glowWoolAnchor, glowWoolItem);
				glowWoolAnchor = glowWoolItem;
			}
		}
		moveAfter(registry, Items.IRON_BOOTS, GranulesMod.HARD_HAT);
		if (!ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
			moveAfter(registry, Items.LEATHER_BOOTS, PalePeltEquipment.TRAPPER_HAT);
			moveAfter(registry, PalePeltEquipment.TRAPPER_HAT, PalePeltEquipment.FUR_BOOTS);
		}
	}

	private static void moveAfter(EntryRegistry registry, ItemLike anchor, ItemLike entry) {
		EntryStack<?> entryStack = EntryStacks.of(entry);
		registry.removeEntry(entryStack);
		registry.addEntryAfter(EntryStacks.of(anchor), entryStack);
	}

}
