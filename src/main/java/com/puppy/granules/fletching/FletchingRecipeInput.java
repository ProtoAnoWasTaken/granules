package com.puppy.granules.fletching;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record FletchingRecipeInput(ItemStack shaft, ItemStack fletch, ItemStack head) implements RecipeInput {
	@Override
	public ItemStack getItem(int slot) {
		return switch (slot) {
			case 0 -> this.shaft;
			case 1 -> this.fletch;
			case 2 -> this.head;
			default -> ItemStack.EMPTY;
		};
	}

	@Override
	public int size() {
		return 3;
	}
}
