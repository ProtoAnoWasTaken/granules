package com.puppy.granules.fletching;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.puppy.granules.GranulesMod;

import java.util.List;

public record FletchingRecipe(
	Ingredient shaft,
	Ingredient fletch,
	Ingredient head,
	ItemStackTemplate result
) implements Recipe<FletchingRecipeInput> {
	public static final MapCodec<FletchingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Ingredient.CODEC.fieldOf("shaft").forGetter(FletchingRecipe::shaft),
		Ingredient.CODEC.fieldOf("fletch").forGetter(FletchingRecipe::fletch),
		Ingredient.CODEC.fieldOf("head").forGetter(FletchingRecipe::head),
		ItemStackTemplate.CODEC.fieldOf("result").forGetter(FletchingRecipe::result)
	).apply(instance, FletchingRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, FletchingRecipe> STREAM_CODEC = StreamCodec.composite(
		Ingredient.CONTENTS_STREAM_CODEC,
		FletchingRecipe::shaft,
		Ingredient.CONTENTS_STREAM_CODEC,
		FletchingRecipe::fletch,
		Ingredient.CONTENTS_STREAM_CODEC,
		FletchingRecipe::head,
		ItemStackTemplate.STREAM_CODEC,
		FletchingRecipe::result,
		FletchingRecipe::new
	);

	@Override
	public boolean matches(FletchingRecipeInput input, Level level) {
		return this.shaft.test(input.shaft())
			&& this.fletch.test(input.fletch())
			&& this.head.test(input.head());
	}

	@Override
	public ItemStack assemble(FletchingRecipeInput input) {
		return this.result.create();
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
	public RecipeSerializer<FletchingRecipe> getSerializer() {
		return GranulesMod.FLETCHING_RECIPE_SERIALIZER;
	}

	@Override
	public RecipeType<FletchingRecipe> getType() {
		return GranulesMod.FLETCHING_RECIPE_TYPE;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.create(List.of(this.shaft, this.fletch, this.head));
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_EQUIPMENT;
	}
}
