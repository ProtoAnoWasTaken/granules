package com.puppy.granules.mixin.access;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandBlockEntityAccessor {
	@Accessor("items")
	NonNullList<ItemStack> granules$getItems();

	@Accessor("brewTime")
	int granules$getBrewTime();

	@Accessor("brewTime")
	void granules$setBrewTime(int brewTime);

	@Accessor("lastPotionCount")
	boolean[] granules$getLastPotionCount();

	@Accessor("lastPotionCount")
	void granules$setLastPotionCount(boolean[] lastPotionCount);

	@Accessor("ingredient")
	Item granules$getIngredient();

	@Accessor("ingredient")
	void granules$setIngredient(Item ingredient);

	@Accessor("fuel")
	int granules$getFuel();

	@Accessor("fuel")
	void granules$setFuel(int fuel);
}
