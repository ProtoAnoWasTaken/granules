package com.puppy.granules.compat;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.fletching.FletchingRecipe;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public final class FletchingDisplay extends BasicDisplay {
	public static final CategoryIdentifier<FletchingDisplay> CATEGORY = CategoryIdentifier.of(GranulesMod.MOD_ID, "fletching");
	public static final DisplaySerializer<FletchingDisplay> SERIALIZER = DisplaySerializer.of(
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(FletchingDisplay::getInputEntries),
			EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(FletchingDisplay::getOutputEntries),
			Identifier.CODEC.optionalFieldOf("location").forGetter(FletchingDisplay::getDisplayLocation)
		).apply(instance, FletchingDisplay::new)),
		StreamCodec.composite(
			EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
			FletchingDisplay::getInputEntries,
			EntryIngredient.streamCodec().apply(ByteBufCodecs.list()),
			FletchingDisplay::getOutputEntries,
			ByteBufCodecs.optional(Identifier.STREAM_CODEC),
			FletchingDisplay::getDisplayLocation,
			FletchingDisplay::new
		)
	);

	public FletchingDisplay(RecipeHolder<FletchingRecipe> recipe) {
		this(
			List.of(
				EntryIngredients.ofIngredient(recipe.value().shaft()),
				EntryIngredients.ofIngredient(recipe.value().fletch()),
				EntryIngredients.ofIngredient(recipe.value().head())
			),
			List.of(EntryIngredients.of(recipe.value().result())),
			Optional.of(recipe.id().identifier())
		);
	}

	public FletchingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location) {
		super(inputs, outputs, location);
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return CATEGORY;
	}

	@Override
	public DisplaySerializer<? extends Display> getSerializer() {
		return SERIALIZER;
	}

	public EntryIngredient shaft() {
		return getInputEntries().get(0);
	}

	public EntryIngredient fletch() {
		return getInputEntries().get(1);
	}

	public EntryIngredient head() {
		return getInputEntries().get(2);
	}

	public EntryIngredient result() {
		return getOutputEntries().getFirst();
	}
}
