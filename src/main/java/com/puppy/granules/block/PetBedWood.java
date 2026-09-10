package com.puppy.granules.block;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum PetBedWood implements StringRepresentable {
	OAK("oak_log", "oak_log", "oak_wood"),
	STRIPPED_OAK("stripped_oak_log", "stripped_oak_log", "stripped_oak_wood"),
	SPRUCE("spruce_log", "spruce_log", "spruce_wood"),
	STRIPPED_SPRUCE("stripped_spruce_log", "stripped_spruce_log", "stripped_spruce_wood"),
	BIRCH("birch_log", "birch_log", "birch_wood"),
	STRIPPED_BIRCH("stripped_birch_log", "stripped_birch_log", "stripped_birch_wood"),
	JUNGLE("jungle_log", "jungle_log", "jungle_wood"),
	STRIPPED_JUNGLE("stripped_jungle_log", "stripped_jungle_log", "stripped_jungle_wood"),
	ACACIA("acacia_log", "acacia_log", "acacia_wood"),
	STRIPPED_ACACIA("stripped_acacia_log", "stripped_acacia_log", "stripped_acacia_wood"),
	DARK_OAK("dark_oak_log", "dark_oak_log", "dark_oak_wood"),
	STRIPPED_DARK_OAK("stripped_dark_oak_log", "stripped_dark_oak_log", "stripped_dark_oak_wood"),
	MANGROVE("mangrove_log", "mangrove_log", "mangrove_wood"),
	STRIPPED_MANGROVE("stripped_mangrove_log", "stripped_mangrove_log", "stripped_mangrove_wood"),
	CHERRY("cherry_log", "cherry_log", "cherry_wood"),
	STRIPPED_CHERRY("stripped_cherry_log", "stripped_cherry_log", "stripped_cherry_wood"),
	PALE_OAK("pale_oak_log", "pale_oak_log", "pale_oak_wood"),
	STRIPPED_PALE_OAK("stripped_pale_oak_log", "stripped_pale_oak_log", "stripped_pale_oak_wood"),
	CRIMSON("crimson_stem", "crimson_stem", "crimson_hyphae"),
	STRIPPED_CRIMSON("stripped_crimson_stem", "stripped_crimson_stem", "stripped_crimson_hyphae"),
	WARPED("warped_stem", "warped_stem", "warped_hyphae"),
	STRIPPED_WARPED("stripped_warped_stem", "stripped_warped_stem", "stripped_warped_hyphae"),
	BAMBOO("bamboo_block", "bamboo_block"),
	STRIPPED_BAMBOO("stripped_bamboo_block", "stripped_bamboo_block");

	private final String serializedName;
	private final String[] acceptedItemPaths;

	PetBedWood(String serializedName, String... acceptedItemPaths) {
		this.serializedName = serializedName;
		this.acceptedItemPaths = acceptedItemPaths;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	public static Optional<PetBedWood> fromStack(ItemStack stack) {
		return fromItem(stack.getItem());
	}

	public static Optional<PetBedWood> fromItem(Item item) {
		String itemPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
		return Arrays.stream(values())
			.filter(wood -> Arrays.asList(wood.acceptedItemPaths).contains(itemPath))
			.findFirst();
	}

	public static Item[] valuesAsItems() {
		return Arrays.stream(values())
			.map(wood -> BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(wood.serializedName)))
			.toArray(Item[]::new);
	}
}
