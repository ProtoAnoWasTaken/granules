package com.puppy.granules.mixin;

import com.puppy.granules.mixin.access.BrewingStandBlockEntityAccessor;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBatchMixin {
	@Unique
	private static final int GRANULES_FUEL_USES_PER_POWDER = 20;

	@Unique
	private static final int GRANULES_BREW_TIME = 400;

	@Unique
	private int granules$batchCost;

	@Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
	private static void granules$brewStackedPotions(
		Level level,
		BlockPos pos,
		BlockState state,
		BrewingStandBlockEntity brewingStand,
		CallbackInfo callbackInfo
	) {
		BrewingStandBlockEntityAccessor accessor = (BrewingStandBlockEntityAccessor) brewingStand;
		NonNullList<ItemStack> items = accessor.granules$getItems();
		PotionBrewing potionBrewing = level.potionBrewing();
		BrewingStandBatchMixin batchMixin = (BrewingStandBatchMixin) (Object) brewingStand;
		int currentBatchCost = granules$batchCost(potionBrewing, items);
		boolean canBrew = currentBatchCost > 0 && items.get(3).getCount() >= currentBatchCost;
		if (accessor.granules$getBrewTime() > 0) {
			accessor.granules$setBrewTime(accessor.granules$getBrewTime() - 1);
			if (accessor.granules$getBrewTime() == 0) {
				if (
					canBrew
						&& currentBatchCost == batchMixin.granules$batchCost
						&& items.get(3).is(accessor.granules$getIngredient())
				) {
					granules$doBatchBrew(level, pos, items, potionBrewing, currentBatchCost);
				}
				batchMixin.granules$batchCost = 0;
				brewingStand.setChanged();
			} else if (!canBrew || !items.get(3).is(accessor.granules$getIngredient())) {
				accessor.granules$setBrewTime(0);
				batchMixin.granules$batchCost = 0;
				brewingStand.setChanged();
			}
		} else if (canBrew && granules$consumeFuelForBatch(accessor, items.get(4), currentBatchCost)) {
			accessor.granules$setBrewTime(GRANULES_BREW_TIME);
			accessor.granules$setIngredient(items.get(3).getItem());
			batchMixin.granules$batchCost = currentBatchCost;
			brewingStand.setChanged();
		}
		granules$updateBottleState(level, pos, state, accessor, items);
		callbackInfo.cancel();
	}

	@Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
	private void granules$allowPotionStacksInBottleSlots(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (slot < 0 || slot > 2 || !granules$isPotionContainer(stack)) {
			return;
		}
		BrewingStandBlockEntityAccessor accessor = (BrewingStandBlockEntityAccessor) (Object) this;
		ItemStack existingStack = accessor.granules$getItems().get(slot);
		callbackInfo.setReturnValue(
			existingStack.isEmpty()
				|| ItemStack.isSameItemSameComponents(existingStack, stack) && existingStack.getCount() < existingStack.getMaxStackSize()
		);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void granules$loadBatchCost(ValueInput input, CallbackInfo callbackInfo) {
		granules$batchCost = input.getIntOr("GranulesBatchCost", 0);
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void granules$saveBatchCost(ValueOutput output, CallbackInfo callbackInfo) {
		output.putInt("GranulesBatchCost", granules$batchCost);
	}

	@Unique
	private static int granules$batchCost(PotionBrewing potionBrewing, NonNullList<ItemStack> items) {
		ItemStack ingredient = items.get(3);
		if (ingredient.isEmpty() || !potionBrewing.isIngredient(ingredient)) {
			return 0;
		}
		int bottleCount = 0;
		for (int slot = 0; slot < 3; slot++) {
			ItemStack potion = items.get(slot);
			if (potionBrewing.hasMix(potion, ingredient)) {
				bottleCount += potion.getCount();
			}
		}
		return bottleCount == 0 ? 0 : (bottleCount + 2) / 3;
	}

	@Unique
	private static boolean granules$consumeFuelForBatch(
		BrewingStandBlockEntityAccessor accessor,
		ItemStack fuelStack,
		int batchCost
	) {
		int fuel = accessor.granules$getFuel();
		int missingFuel = batchCost - fuel;
		if (missingFuel > 0) {
			int powderCount = (missingFuel + GRANULES_FUEL_USES_PER_POWDER - 1) / GRANULES_FUEL_USES_PER_POWDER;
			if (!fuelStack.is(ItemTags.BREWING_FUEL) || fuelStack.getCount() < powderCount) {
				return false;
			}
			fuelStack.shrink(powderCount);
			fuel += powderCount * GRANULES_FUEL_USES_PER_POWDER;
		}
		accessor.granules$setFuel(fuel - batchCost);
		return true;
	}

	@Unique
	private static void granules$doBatchBrew(
		Level level,
		BlockPos pos,
		NonNullList<ItemStack> items,
		PotionBrewing potionBrewing,
		int batchCost
	) {
		ItemStack ingredient = items.get(3);
		for (int slot = 0; slot < 3; slot++) {
			ItemStack input = items.get(slot);
			if (!potionBrewing.hasMix(input, ingredient)) {
				continue;
			}
			ItemStack output = potionBrewing.mix(ingredient, input);
			output.setCount(input.getCount());
			items.set(slot, output);
		}
		granules$consumeIngredient(level, pos, items, batchCost);
		level.levelEvent(1035, pos, 0);
	}

	@Unique
	private static void granules$consumeIngredient(Level level, BlockPos pos, NonNullList<ItemStack> items, int batchCost) {
		ItemStack ingredient = items.get(3);
		ItemStackTemplate craftingRemainder = ingredient.getItem().getCraftingRemainder();
		ingredient.shrink(batchCost);
		if (craftingRemainder == null) {
			return;
		}
		ItemStack remainder = craftingRemainder.create();
		remainder.setCount(batchCost);
		if (ingredient.isEmpty()) {
			items.set(3, remainder);
			return;
		}
		Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder);
	}

	@Unique
	private static void granules$updateBottleState(
		Level level,
		BlockPos pos,
		BlockState state,
		BrewingStandBlockEntityAccessor accessor,
		NonNullList<ItemStack> items
	) {
		boolean[] currentPotionCount = new boolean[3];
		for (int slot = 0; slot < 3; slot++) {
			currentPotionCount[slot] = !items.get(slot).isEmpty();
		}
		if (Arrays.equals(currentPotionCount, accessor.granules$getLastPotionCount())) {
			return;
		}
		accessor.granules$setLastPotionCount(currentPotionCount);
		if (!(state.getBlock() instanceof BrewingStandBlock)) {
			return;
		}
		BlockState updatedState = state;
		for (int slot = 0; slot < 3; slot++) {
			updatedState = updatedState.setValue(BrewingStandBlock.HAS_BOTTLE[slot], currentPotionCount[slot]);
		}
		level.setBlock(pos, updatedState, 2);
	}

	@Unique
	private static boolean granules$isPotionContainer(ItemStack stack) {
		return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
	}
}
