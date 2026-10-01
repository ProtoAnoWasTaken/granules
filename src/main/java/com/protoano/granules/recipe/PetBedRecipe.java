package com.protoano.granules.recipe;

import com.mojang.serialization.MapCodec;
import com.protoano.granules.GranulesMod;
import com.protoano.granules.block.PetBedWood;
import com.protoano.granules.item.PetBedItem;
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
	public static final MapCodec<PetBedRecipe> MAP_CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
		PetBedWood.CODEC.optionalFieldOf("wood", PetBedWood.OAK).forGetter(recipe -> recipe.wood),
		DyeColor.CODEC.optionalFieldOf("wool", DyeColor.WHITE).forGetter(recipe -> recipe.wool)
	).apply(instance, PetBedRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, PetBedRecipe> STREAM_CODEC = net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());
	private final PetBedWood wood;
	private final DyeColor wool;

	public PetBedRecipe(PetBedWood wood, DyeColor wool) {
		this.wood = wood;
		this.wool = wool;
	}

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
		if (firstLog.orElse(null) != wood || !matchesWood(input.getItem(2, 1), wood) || !matchesWood(input.getItem(0, 2), wood) || !matchesWood(input.getItem(2, 2), wood)) {
			return false;
		}
		DyeColor carpetColor = carpetColor(input.getItem(1, 1));
		DyeColor woolColor = woolColor(input.getItem(1, 2));
		return carpetColor == wool && woolColor == wool;
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
		return "granules:pet_bed";
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
		Ingredient logs = Ingredient.of(wood.materials());
		Ingredient carpets = Ingredient.of(Blocks.CARPET.pick(wool));
		Ingredient wools = Ingredient.of(Blocks.WOOL.pick(wool));
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

	@Override
	public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
		var empty = net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE;
		var log = Ingredient.of(wood.materials()).display();
		var totem = Ingredient.of(Items.TOTEM_OF_UNDYING).display();
		var carpet = Ingredient.of(Blocks.CARPET.pick(wool)).display();
		var woolBlock = Ingredient.of(Blocks.WOOL.pick(wool)).display();
		var result = new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(
			net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(PetBedItem.createStack(wood, wool)));
		return List.of(new net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay(3, 3,
			List.of(empty, totem, empty, log, carpet, log, log, woolBlock, log), result,
			Ingredient.of(Blocks.CRAFTING_TABLE).display()));
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
