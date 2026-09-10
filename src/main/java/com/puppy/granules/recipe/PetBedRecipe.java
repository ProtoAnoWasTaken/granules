package com.puppy.granules.recipe;

import com.mojang.serialization.MapCodec;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.block.PetBedWood;
import com.puppy.granules.item.PetBedItem;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class PetBedRecipe implements CraftingRecipe {
	public static final MapCodec<PetBedRecipe> MAP_CODEC = MapCodec.unit(new PetBedRecipe());
	public static final StreamCodec<RegistryFriendlyByteBuf, PetBedRecipe> STREAM_CODEC = StreamCodec.unit(new PetBedRecipe());

	@Override
	public boolean matches(CraftingInput input, Level level) {
		if (input.width() != 3 || input.height() != 3) {
			return false;
		}
		if (!input.getItem(0, 0).isEmpty() || !input.getItem(2, 0).isEmpty()) {
			return false;
		}
		if (!input.getItem(1, 0).is(Items.TOTEM_OF_UNDYING)) {
			return false;
		}
		Optional<PetBedWood> firstLog = PetBedWood.fromStack(input.getItem(0, 1));
		if (firstLog.isEmpty() || !matchesWood(input.getItem(2, 1), firstLog.get()) || !matchesWood(input.getItem(0, 2), firstLog.get()) || !matchesWood(input.getItem(2, 2), firstLog.get())) {
			return false;
		}
		DyeColor carpetColor = carpetColor(input.getItem(1, 1));
		DyeColor woolColor = woolColor(input.getItem(1, 2));
		return carpetColor != null && carpetColor == woolColor;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		PetBedWood wood = PetBedWood.fromStack(input.getItem(0, 1)).orElse(PetBedWood.OAK);
		DyeColor wool = woolColor(input.getItem(1, 2));
		return PetBedItem.createStack(wood, wool == null ? DyeColor.WHITE : wool);
	}

	@Override
	public boolean showNotification() {
		return true;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<PetBedRecipe> getSerializer() {
		return GranulesMod.PET_BED_RECIPE_SERIALIZER;
	}

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.BUILDING;
	}

	@Override
	public PlacementInfo placementInfo() {
		Ingredient logs = Ingredient.of(PetBedWood.valuesAsItems());
		Ingredient carpets = Ingredient.of(java.util.Arrays.stream(DyeColor.values()).map(color -> Blocks.CARPET.pick(color)));
		Ingredient wools = Ingredient.of(java.util.Arrays.stream(DyeColor.values()).map(color -> Blocks.WOOL.pick(color)));
		return PlacementInfo.createFromOptionals(List.of(
			Optional.empty(),
			Optional.of(Ingredient.of(Items.TOTEM_OF_UNDYING)),
			Optional.empty(),
			Optional.of(logs),
			Optional.of(carpets),
			Optional.of(logs),
			Optional.of(logs),
			Optional.of(wools),
			Optional.of(logs)
		));
	}

	private static boolean matchesWood(ItemStack stack, PetBedWood expected) {
		return PetBedWood.fromStack(stack).orElse(null) == expected;
	}

	private static DyeColor woolColor(ItemStack stack) {
		for (DyeColor color : DyeColor.values()) {
			if (stack.is(Blocks.WOOL.pick(color).asItem())) {
				return color;
			}
		}
		return null;
	}

	private static DyeColor carpetColor(ItemStack stack) {
		for (DyeColor color : DyeColor.values()) {
			if (stack.is(Blocks.CARPET.pick(color).asItem())) {
				return color;
			}
		}
		return null;
	}
}
