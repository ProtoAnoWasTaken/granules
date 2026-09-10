package com.puppy.granules.fletching;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import com.puppy.granules.advancement.GranulesAdvancements;

import java.util.List;
import java.util.Optional;

public class FletchingMenu extends ItemCombinerMenu {
	public static final int SHAFT_SLOT = 0;
	public static final int FLETCH_SLOT = 1;
	public static final int HEAD_SLOT = 2;
	public static final int RESULT_SLOT = 3;
	public static final int SHAFT_SLOT_X = 26;
	public static final int FLETCH_SLOT_X = 8;
	public static final int HEAD_SLOT_X = 44;
	public static final int RESULT_SLOT_X = 98;
	public static final int SLOT_Y = 48;
	private final Level level;
	private final DataSlot hasRecipeError = DataSlot.standalone();

	public FletchingMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public FletchingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		this(containerId, inventory, access, inventory.player.level());
	}

	private FletchingMenu(int containerId, Inventory inventory, ContainerLevelAccess access, Level level) {
		super(GranulesMod.FLETCHING_MENU, containerId, inventory, access, createInputSlotDefinitions());
		this.level = level;
		this.addDataSlot(this.hasRecipeError).set(0);
	}

	private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
		return ItemCombinerMenuSlotDefinition.create()
			.withSlot(SHAFT_SLOT, SHAFT_SLOT_X, SLOT_Y, stack -> true)
			.withSlot(FLETCH_SLOT, FLETCH_SLOT_X, SLOT_Y, stack -> true)
			.withSlot(HEAD_SLOT, HEAD_SLOT_X, SLOT_Y, stack -> true)
			.withResultSlot(RESULT_SLOT, RESULT_SLOT_X, SLOT_Y)
			.build();
	}

	@Override
	protected boolean isValidBlock(BlockState state) {
		return state.is(Blocks.FLETCHING_TABLE) || state.is(GranulesMod.ENCHANTED_FLETCHING_TABLE);
	}

	@Override
	protected void onTake(Player player, ItemStack carried) {
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
			&& carried.getItem() instanceof FletchersArrowItem) {
			ArrowParts parts = FletchersArrowItem.partsOf(carried);
			long combinations = ArrowParts.ALL.stream().filter(candidate -> !candidate.isBasic()).count();
			GranulesAdvancements.recordArrowCombination(serverPlayer, parts.identifierSuffix(), (int) combinations);
		}
		carried.onCraftedBy(player, carried.getCount());
		this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
		int retainedSlot = this.selectRetainedIngredient(player);
		if (retainedSlot != SHAFT_SLOT) {
			this.shrinkStackInSlot(SHAFT_SLOT);
		}
		if (retainedSlot != FLETCH_SLOT) {
			this.shrinkStackInSlot(FLETCH_SLOT);
		}
		if (retainedSlot != HEAD_SLOT) {
			this.shrinkStackInSlot(HEAD_SLOT);
		}
		this.access.execute((level, pos) -> level.playSound(
			null,
			(BlockPos) pos,
			SoundEvents.VILLAGER_WORK_FLETCHER,
			SoundSource.BLOCKS,
			1.0F,
			1.0F
		));
	}

	private int selectRetainedIngredient(Player player) {
		if (player.level().isClientSide() || !this.isEnchantedFletchingTable() || player.getRandom().nextInt(6) != 0) {
			return -1;
		}
		return player.getRandom().nextInt(3);
	}

	private boolean isEnchantedFletchingTable() {
		return EnchantedWorkstationAccess.isAt(this.access, GranulesMod.ENCHANTED_FLETCHING_TABLE);
	}

	private List<ItemStack> getRelevantItems() {
		return List.of(
			this.inputSlots.getItem(SHAFT_SLOT),
			this.inputSlots.getItem(FLETCH_SLOT),
			this.inputSlots.getItem(HEAD_SLOT)
		);
	}

	private void shrinkStackInSlot(int slot) {
		ItemStack stack = this.inputSlots.getItem(slot);
		if (!stack.isEmpty()) {
			stack.shrink(1);
			this.inputSlots.setItem(slot, stack);
		}
	}

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);
		if (this.level instanceof ServerLevel) {
			boolean allInputsPresent = this.getSlot(SHAFT_SLOT).hasItem()
				&& this.getSlot(FLETCH_SLOT).hasItem()
				&& this.getSlot(HEAD_SLOT).hasItem();
			this.hasRecipeError.set(allInputsPresent && !this.getSlot(RESULT_SLOT).hasItem() ? 1 : 0);
		}
	}

	@Override
	public void createResult() {
		FletchingRecipeInput input = new FletchingRecipeInput(
			this.inputSlots.getItem(SHAFT_SLOT),
			this.inputSlots.getItem(FLETCH_SLOT),
			this.inputSlots.getItem(HEAD_SLOT)
		);
		if (this.level instanceof ServerLevel serverLevel) {
			Optional<RecipeHolder<FletchingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(
				GranulesMod.FLETCHING_RECIPE_TYPE,
				input,
				serverLevel
			);
			recipe.ifPresentOrElse(found -> {
				ItemStack result = this.copyArrowIngredientData(found.value().assemble(input), input);
				this.resultSlots.setRecipeUsed(found);
				this.resultSlots.setItem(0, result);
			}, () -> {
				this.resultSlots.setRecipeUsed(null);
				this.resultSlots.setItem(0, this.createFletchersArrowResult(input));
			});
		}
	}

	private ItemStack createFletchersArrowResult(FletchingRecipeInput input) {
		Optional<ArrowParts> parts = ArrowParts.fromIngredients(input.shaft(), input.fletch(), input.head());
		if (parts.isEmpty() || parts.get().isBasic()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = FletchersArrowItem.createStack(parts.get(), 4);
		return this.copyArrowIngredientData(result, input);
	}

	private ItemStack copyArrowIngredientData(ItemStack result, FletchingRecipeInput input) {
		if (!(result.getItem() instanceof FletchersArrowItem)) {
			return result;
		}
		ArrowParts parts = FletchersArrowItem.partsOf(result);
		if (parts.fletch() == ArrowParts.Fletch.NAME_TAG && input.fletch().has(DataComponents.CUSTOM_NAME)) {
			result.set(DataComponents.CUSTOM_NAME, input.fletch().get(DataComponents.CUSTOM_NAME));
		}
		if (parts.fletch() == ArrowParts.Fletch.FIREWORK && input.fletch().has(DataComponents.FIREWORKS)) {
			result.set(DataComponents.FIREWORKS, input.fletch().get(DataComponents.FIREWORKS));
		}
		return result;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
	}

	public boolean hasRecipeError() {
		return this.hasRecipeError.get() > 0;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		if (slotIndex > RESULT_SLOT && slotIndex < this.slots.size()) {
			Slot sourceSlot = this.getSlot(slotIndex);
			if (sourceSlot.hasItem()) {
				ItemStack sourceStack = sourceSlot.getItem();
				int inputSlot = ArrowParts.inputSlotFor(sourceStack);
				if (inputSlot >= 0) {
					ItemStack copiedStack = sourceStack.copy();
					if (!this.moveItemStackTo(sourceStack, inputSlot, inputSlot + 1, false)) {
						return ItemStack.EMPTY;
					}
					if (sourceStack.isEmpty()) {
						sourceSlot.setByPlayer(ItemStack.EMPTY);
					} else {
						sourceSlot.setChanged();
					}
					sourceSlot.onTake(player, sourceStack);
					return copiedStack;
				}
			}
		}
		return super.quickMoveStack(player, slotIndex);
	}

	@Override
	protected boolean canMoveIntoInputSlots(ItemStack stack) {
		return ArrowParts.inputSlotFor(stack) >= 0;
	}
}
