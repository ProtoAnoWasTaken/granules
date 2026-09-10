package com.puppy.granules.block;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedCauldronProperties;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PlanterBlockEntity extends BlockEntity implements WorldlyContainer {
	private static final int OUTPUT_START = 0;
	private static final int OUTPUT_END = 2;
	private static final int SOIL_SLOT = 3;
	private static final int CROP_SLOT = 4;
	private static final int INPUT_SLOT = 5;
	private static final int[] OUTPUT_SLOTS = new int[] {0, 1, 2};
	private static final int[] INPUT_SLOTS = new int[] {INPUT_SLOT};
	private final NonNullList<ItemStack> items = NonNullList.withSize(7, ItemStack.EMPTY);
	private int moisture;
	private int growthAge;
	private int growthTicks;
	private boolean tilled;

	public PlanterBlockEntity(BlockPos pos, BlockState state) {
		super(GranulesMod.PLANTER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, PlanterBlockEntity planter) {
		planter.tickServer();
	}

	private void tickServer() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		updateBlockAppearance();
		if (serverLevel.getGameTime() % 4L == 0L) {
			processAutomationInput();
		}
		CropInfo crop = getCropInfo();
		if (crop == null) {
			return;
		}
		if (isMature(crop)) {
			if (level.getBlockEntity(worldPosition.below()) instanceof HopperBlockEntity) {
				harvest(null, ItemStack.EMPTY, true);
			}
			return;
		}
		if (crop.requiresWater() && moisture == 0) {
			if (serverLevel.getGameTime() % 20L != 0L || !drawWaterFromNearbyCauldron(serverLevel)) {
				return;
			}
			moisture = 3;
			setChangedAndSync();
		}
		growthTicks++;
		int requiredTicks = isEnchanted() ? 80 : 120;
		if (growthTicks < requiredTicks) {
			return;
		}
		growthTicks = 0;
		growthAge = Math.min(crop.maxAge(), growthAge + 1);
		int waterLossBound = isEnchanted() ? 6 : 3;
		if (crop.requiresWater() && serverLevel.getRandom().nextInt(waterLossBound) == 0) {
			moisture--;
		}
		setChangedAndSync();
		if (isMature(crop) && level.getBlockEntity(worldPosition.below()) instanceof HopperBlockEntity) {
			harvest(null, ItemStack.EMPTY, true);
		}
	}

	public boolean interact(Player player, InteractionHand hand, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		CropInfo crop = getCropInfo();
		if (stack.getItem() instanceof ShovelItem) {
			if (crop != null) {
				removeCrop(serverPlayer, hand, stack);
				return true;
			}
			if (!getSoil().isEmpty()) {
				removeSoil(serverPlayer, hand, stack);
				return true;
			}
		}
		if (stack.is(Items.BONE_MEAL) && crop != null && !isMature(crop)) {
			return applyBoneMeal(serverPlayer, stack);
		}
		if (crop != null && isMature(crop)) {
			return harvest(serverPlayer, stack, false);
		}
		if (isWaterPotion(stack)) {
			return addWater(serverPlayer, hand, stack);
		}
		if (getSoil().isEmpty() && isSoil(stack)) {
			setItem(SOIL_SLOT, stack.consumeAndReturn(1, player));
			tilled = false;
			setChangedAndSync();
			level.playSound(null, worldPosition, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.7F, 0.9F);
			return true;
		}
		if (crop == null && stack.getItem() instanceof HoeItem && canTill()) {
			tilled = true;
			stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
			setChangedAndSync();
			level.playSound(null, worldPosition, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			return true;
		}
		CropInfo plantedCrop = getCropInfo(stack);
		if (crop == null && plantedCrop != null && plantedCrop.acceptsSoil(getSoil(), tilled)) {
			ItemStack plantedStack = stack.copyWithCount(1);
			setItem(CROP_SLOT, stack.consumeAndReturn(1, player));
			growthAge = 0;
			growthTicks = 0;
			setChangedAndSync();
			level.playSound(null, worldPosition, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8F, 1.0F);
			CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, worldPosition, plantedStack);
			return true;
		}
		return false;
	}

	public boolean useEmptyHand(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		CropInfo crop = getCropInfo();
		if (crop != null && isMature(crop)) {
			return harvest(serverPlayer, ItemStack.EMPTY, false);
		}
		return takeOutput(serverPlayer);
	}

	private boolean addWater(ServerPlayer player, InteractionHand hand, ItemStack stack) {
		if (moisture >= 3) {
			return false;
		}
		moisture = 3;
		player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
		setChangedAndSync();
		level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.9F, 1.0F);
		return true;
	}

	private void removeSoil(ServerPlayer player, InteractionHand hand, ItemStack stack) {
		ItemStack soil = removeItemNoUpdate(SOIL_SLOT);
		if (!player.getInventory().add(soil)) {
			player.drop(soil, false);
		}
		stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		tilled = false;
		moisture = 0;
		setChangedAndSync();
		level.playSound(null, worldPosition, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 0.8F, 0.9F);
	}

	private void removeCrop(ServerPlayer player, InteractionHand hand, ItemStack stack) {
		ItemStack crop = removeItemNoUpdate(CROP_SLOT);
		if (!player.getInventory().add(crop)) {
			player.drop(crop, false);
		}
		growthAge = 0;
		growthTicks = 0;
		stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		setChangedAndSync();
		level.playSound(null, worldPosition, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 0.8F, 1.0F);
	}

	private boolean harvest(ServerPlayer player, ItemStack tool, boolean intoStorage) {
		CropInfo crop = getCropInfo();
		if (crop == null || !isMature(crop)) {
			return false;
		}
		List<ItemStack> drops = getHarvestDrops(crop, player, tool);
		HopperBlockEntity hopper = intoStorage && level.getBlockEntity(worldPosition.below()) instanceof HopperBlockEntity belowHopper
			? belowHopper
			: null;
		if (intoStorage && hopper == null) {
			return false;
		}
		if (hopper != null && !canInsertAll(hopper, drops)) {
			return false;
		}
		for (ItemStack drop : drops) {
			if (drop.isEmpty()) {
				continue;
			}
			if (hopper != null) {
				HopperBlockEntity.addItem(this, hopper, drop.copy(), Direction.UP);
			} else {
				Block.popResource(level, worldPosition.above(), drop);
			}
		}
		growthAge = 0;
		growthTicks = 0;
		setChangedAndSync();
		level.playSound(null, worldPosition, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
		return true;
	}

	private List<ItemStack> getHarvestDrops(CropInfo crop, ServerPlayer player, ItemStack tool) {
		List<ItemStack> sourceDrops = new ArrayList<>();
		if (crop.lootBlock() != null && level instanceof ServerLevel serverLevel) {
			BlockState matureState = crop.matureState();
			sourceDrops.addAll(Block.getDrops(matureState, serverLevel, worldPosition, null, player, tool));
		} else {
			int fortuneLevel = 0;
			if (!tool.isEmpty() && level instanceof ServerLevel serverLevel) {
				fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(
					serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),
					tool
				);
			}
			sourceDrops.add(new ItemStack(crop.harvestItem(), 3 + fortuneLevel));
		}
		removeReplantingItem(sourceDrops, crop.plantingItem());
		List<ItemStack> reducedDrops = new ArrayList<>();
		for (ItemStack drop : sourceDrops) {
			int reducedCount = drop.getCount() / 2;
			if ((drop.getCount() & 1) == 1 && level.getRandom().nextBoolean()) {
				reducedCount++;
			}
			if (reducedCount > 0) {
				ItemStack reduced = drop.copy();
				reduced.setCount(reducedCount);
				reducedDrops.add(reduced);
			}
		}
		return reducedDrops;
	}

	private static void removeReplantingItem(List<ItemStack> drops, Item plantingItem) {
		for (ItemStack drop : drops) {
			if (drop.is(plantingItem) && !drop.isEmpty()) {
				drop.shrink(1);
				return;
			}
		}
	}

	private boolean canInsertAll(HopperBlockEntity hopper, List<ItemStack> drops) {
		NonNullList<ItemStack> original = NonNullList.withSize(hopper.getContainerSize(), ItemStack.EMPTY);
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			original.set(slot, hopper.getItem(slot).copy());
		}
		for (ItemStack drop : drops) {
			ItemStack remaining = HopperBlockEntity.addItem(this, hopper, drop.copy(), Direction.UP);
			if (!remaining.isEmpty()) {
				for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
					hopper.setItem(slot, original.get(slot));
				}
				return false;
			}
		}
		for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
			hopper.setItem(slot, original.get(slot));
		}
		return true;
	}

	private boolean takeOutput(ServerPlayer player) {
		boolean tookItem = false;
		for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
			ItemStack output = removeItemNoUpdate(slot);
			if (output.isEmpty()) {
				continue;
			}
			tookItem = true;
			if (!player.getInventory().add(output)) {
				player.drop(output, false);
			}
		}
		if (tookItem) {
			setChangedAndSync();
		}
		return tookItem;
	}

	private void processAutomationInput() {
		ItemStack input = getItem(INPUT_SLOT);
		if (input.isEmpty()) {
			return;
		}
		boolean changed = false;
		if (getSoil().isEmpty() && isSoil(input)) {
			setItem(SOIL_SLOT, takeSingleAutomationInput());
			tilled = false;
			changed = true;
		} else if (isWaterPotion(input) && moisture < 3 && canStore(new ItemStack(Items.GLASS_BOTTLE))) {
			consumeAutomationInput();
			storeOutput(new ItemStack(Items.GLASS_BOTTLE));
			moisture = 3;
			changed = true;
		} else if (getCropInfo() == null) {
			CropInfo crop = getCropInfo(input);
			if (crop != null && crop.acceptsSoil(getSoil(), tilled)) {
				setItem(CROP_SLOT, takeSingleAutomationInput());
				growthAge = 0;
				growthTicks = 0;
				changed = true;
			}
		}
		if (changed) {
			setChangedAndSync();
		}
	}

	private ItemStack takeSingleAutomationInput() {
		ItemStack input = getItem(INPUT_SLOT);
		ItemStack taken = input.copyWithCount(1);
		input.shrink(1);
		if (input.isEmpty()) {
			setItem(INPUT_SLOT, ItemStack.EMPTY);
		}
		return taken;
	}

	private void consumeAutomationInput() {
		ItemStack input = getItem(INPUT_SLOT);
		input.shrink(1);
		if (input.isEmpty()) {
			setItem(INPUT_SLOT, ItemStack.EMPTY);
		}
	}

	private boolean canStoreAll(List<ItemStack> drops) {
		NonNullList<ItemStack> simulated = NonNullList.withSize(3, ItemStack.EMPTY);
		for (int slot = OUTPUT_START; slot <= OUTPUT_END; slot++) {
			simulated.set(slot, getItem(slot).copy());
		}
		for (ItemStack drop : drops) {
			ItemStack remaining = drop.copy();
			for (int slot = OUTPUT_START; slot <= OUTPUT_END && !remaining.isEmpty(); slot++) {
				ItemStack stored = simulated.get(slot);
				if (stored.isEmpty()) {
					simulated.set(slot, remaining.copy());
					remaining.setCount(0);
				} else if (ItemStack.isSameItemSameComponents(stored, remaining)) {
					int capacity = stored.getMaxStackSize() - stored.getCount();
					int moved = Math.min(capacity, remaining.getCount());
					stored.grow(moved);
					remaining.shrink(moved);
				}
			}
			if (!remaining.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private boolean canStore(ItemStack stack) {
		return canStoreAll(List.of(stack));
	}

	private void storeOutput(ItemStack stack) {
		ItemStack remaining = stack.copy();
		for (int slot = OUTPUT_START; slot <= OUTPUT_END && !remaining.isEmpty(); slot++) {
			ItemStack stored = getItem(slot);
			if (stored.isEmpty()) {
				setItem(slot, remaining.copy());
				remaining.setCount(0);
			} else if (ItemStack.isSameItemSameComponents(stored, remaining)) {
				int moved = Math.min(stored.getMaxStackSize() - stored.getCount(), remaining.getCount());
				stored.grow(moved);
				remaining.shrink(moved);
			}
		}
	}

	private boolean canTill() {
		Block soilBlock = getSoilBlock();
		return !tilled && (isTillableDirt(soilBlock) || soilBlock == Blocks.NETHERRACK || soilBlock == Blocks.SOUL_SAND);
	}

	private boolean isSoil(ItemStack stack) {
		Block block = Block.byItem(stack.getItem());
		return block.defaultBlockState().is(BlockTags.DIRT)
			|| isSand(block)
			|| block == Blocks.NETHERRACK
			|| block == Blocks.SOUL_SAND
			|| block == Blocks.SOUL_SOIL;
	}

	private boolean isWaterPotion(ItemStack stack) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return stack.is(Items.POTION) && contents != null && contents.is(Potions.WATER);
	}

	public boolean moisturizeFromThrownWater() {
		if (moisture >= 3) {
			return false;
		}
		moisture = 3;
		setChangedAndSync();
		return true;
	}

	private boolean applyBoneMeal(ServerPlayer player, ItemStack stack) {
		CropInfo crop = getCropInfo();
		if (crop == null || isMature(crop)) {
			return false;
		}
		growthAge = Math.min(crop.maxAge(), growthAge + 1 + level.getRandom().nextInt(2));
		growthTicks = 0;
		stack.consume(1, player);
		setChangedAndSync();
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, worldPosition, 0);
		return true;
	}

	private boolean drawWaterFromNearbyCauldron(ServerLevel serverLevel) {
		int radius = isEnchanted() ? 2 : 1;
		for (BlockPos cauldronPos : BlockPos.betweenClosed(worldPosition.offset(-radius, -radius, -radius), worldPosition.offset(radius, radius, radius))) {
			BlockState state = serverLevel.getBlockState(cauldronPos);
			if (state.is(Blocks.WATER_CAULDRON)) {
				LayeredCauldronBlock.lowerFillLevel(state, serverLevel, cauldronPos);
				serverLevel.playSound(null, cauldronPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
				return true;
			}
			if (state.is(GranulesMod.ENCHANTED_CAULDRON)
				&& state.getValue(EnchantedCauldronProperties.CONTENT) == EnchantedCauldronProperties.Content.WATER
				&& state.getValue(EnchantedCauldronProperties.LEVEL) > 0) {
				int updatedLevel = state.getValue(EnchantedCauldronProperties.LEVEL) - 1;
				EnchantedCauldronProperties.Content updatedContent = updatedLevel == 0
					? EnchantedCauldronProperties.Content.EMPTY
					: EnchantedCauldronProperties.Content.WATER;
				serverLevel.setBlockAndUpdate(
					cauldronPos,
					state.setValue(EnchantedCauldronProperties.CONTENT, updatedContent)
						.setValue(EnchantedCauldronProperties.LEVEL, updatedLevel)
				);
				serverLevel.playSound(null, cauldronPos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
				return true;
			}
		}
		return false;
	}

	private CropInfo getCropInfo() {
		return getCropInfo(getItem(CROP_SLOT));
	}

	private CropInfo getCropInfo(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (CropInfo crop : CropInfo.STANDARD_CROPS) {
			if (stack.is(crop.plantingItem())) {
				return crop;
			}
		}
		if (isEnchanted()) {
			for (CropInfo crop : CropInfo.ENCHANTED_CROPS) {
				if (stack.is(crop.plantingItem())) {
					return crop;
				}
			}
		}
		return null;
	}

	private Block getSoilBlock() {
		return Block.byItem(getSoil().getItem());
	}

	private boolean isMature(CropInfo crop) {
		return growthAge >= crop.maxAge();
	}

	private boolean isEnchanted() {
		return getBlockState().getBlock() instanceof PlanterBlock planter && planter.isEnchanted();
	}

	private void setChangedAndSync() {
		setChanged();
		if (level != null) {
			updateBlockAppearance();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private void updateBlockAppearance() {
		if (level == null || !(getBlockState().getBlock() instanceof PlanterBlock)) {
			return;
		}
		BlockState currentState = getBlockState();
		BlockState updatedState = currentState.setValue(PlanterBlock.SOIL, visualSoil());
		if (!updatedState.equals(currentState)) {
			level.setBlock(worldPosition, updatedState, Block.UPDATE_CLIENTS);
		}
	}

	private PlanterSoil visualSoil() {
		if (getSoil().isEmpty()) {
			return PlanterSoil.EMPTY;
		}
		Block soilBlock = getSoilBlock();
		if (soilBlock == Blocks.NETHERRACK) {
			return tilled ? PlanterSoil.SOUL_SOIL : PlanterSoil.NETHERRACK;
		}
		if (soilBlock == Blocks.SOUL_SAND) {
			return tilled ? PlanterSoil.SOUL_SOIL : PlanterSoil.SOUL_SAND;
		}
		if (soilBlock == Blocks.SOUL_SOIL) {
			return PlanterSoil.SOUL_SOIL;
		}
		if (isSand(soilBlock)) {
			return PlanterSoil.SAND;
		}
		if (soilBlock == Blocks.PODZOL) {
			return PlanterSoil.PODZOL;
		}
		if (soilBlock == Blocks.GRASS_BLOCK && !tilled) {
			return PlanterSoil.GRASS;
		}
		if (isDirtBase(soilBlock)) {
			if (tilled) {
				return moisture > 0 ? PlanterSoil.WET_FARMLAND : PlanterSoil.FARMLAND;
			}
			return moisture > 0 ? PlanterSoil.MUD : PlanterSoil.DIRT;
		}
		return PlanterSoil.DIRT;
	}

	private static boolean isDirtBase(Block block) {
		return block.defaultBlockState().is(BlockTags.DIRT);
	}

	private static boolean isTillableDirt(Block block) {
		return block == Blocks.DIRT
			|| block == Blocks.GRASS_BLOCK
			|| block == Blocks.DIRT_PATH
			|| block == Blocks.COARSE_DIRT
			|| block == Blocks.ROOTED_DIRT;
	}

	private static boolean isSand(Block block) {
		return block == Blocks.SAND || block == Blocks.RED_SAND;
	}

	public ItemStack getSoil() {
		return getItem(SOIL_SLOT);
	}

	public ItemStack getRenderedCrop() {
		CropInfo crop = getCropInfo();
		if (crop == null) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(crop.displayItem());
	}

	public BlockState getRenderedCropState() {
		CropInfo crop = getCropInfo();
		if (crop == null) {
			return null;
		}
		return crop.renderedState(growthAge);
	}

	public BlockState getRenderedFruitState() {
		CropInfo crop = getCropInfo();
		if (crop == null) {
			return null;
		}
		return crop.renderedFruitState(growthAge);
	}

	public int getGrowthAge() {
		return growthAge;
	}

	public int getMaximumGrowthAge() {
		CropInfo crop = getCropInfo();
		return crop == null ? 0 : crop.maxAge();
	}

	public int getComparatorOutput() {
		CropInfo crop = getCropInfo();
		if (crop == null) {
			return 0;
		}
		return Math.min(15, Math.round(15.0F * growthAge / crop.maxAge()));
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack item : items) {
			if (!item.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack removed = ContainerHelper.removeItem(items, slot, count);
		if (!removed.isEmpty()) {
			setChangedAndSync();
		}
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removed = ContainerHelper.takeItem(items, slot);
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (!stack.isEmpty()) {
			stack.limitSize(getMaxStackSize(stack));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
		setChangedAndSync();
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return direction == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot == INPUT_SLOT && (isSoil(stack) || isWaterPotion(stack) || getCropInfo(stack) != null);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return direction == Direction.DOWN && slot >= OUTPUT_START && slot <= OUTPUT_END;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		moisture = input.getIntOr("Moisture", 0);
		growthAge = input.getIntOr("GrowthAge", 0);
		growthTicks = input.getIntOr("GrowthTicks", 0);
		tilled = input.getBooleanOr("Tilled", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items, true);
		output.putInt("Moisture", moisture);
		output.putInt("GrowthAge", growthAge);
		output.putInt("GrowthTicks", growthTicks);
		output.putBoolean("Tilled", tilled);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	private enum CropSoilRule {
		CULTIVATED,
		BAMBOO,
		SUGAR_CANE,
		CACTUS,
		SOUL,
		BERRIES
	}

	private record CropInfo(
		Item plantingItem,
		Item displayItem,
		Item harvestItem,
		Block lootBlock,
		IntegerProperty ageProperty,
		int maxAge,
		CropSoilRule soilRule,
		Block renderBlock,
		IntegerProperty renderAgeProperty
	) {
		private static final List<CropInfo> STANDARD_CROPS = List.of(
			new CropInfo(Items.WHEAT_SEEDS, Items.WHEAT, Items.WHEAT, Blocks.WHEAT, CropBlock.AGE, 7, CropSoilRule.CULTIVATED, Blocks.WHEAT, CropBlock.AGE),
			new CropInfo(Items.CARROT, Items.CARROT, Items.CARROT, Blocks.CARROTS, CropBlock.AGE, 7, CropSoilRule.CULTIVATED, Blocks.CARROTS, CropBlock.AGE),
			new CropInfo(Items.POTATO, Items.POTATO, Items.POTATO, Blocks.POTATOES, CropBlock.AGE, 7, CropSoilRule.CULTIVATED, Blocks.POTATOES, CropBlock.AGE),
			new CropInfo(Items.BEETROOT_SEEDS, Items.BEETROOT, Items.BEETROOT, Blocks.BEETROOTS, BeetrootBlock.AGE, 3, CropSoilRule.CULTIVATED, Blocks.BEETROOTS, BeetrootBlock.AGE),
			new CropInfo(Items.MELON_SEEDS, Items.MELON_SLICE, Items.MELON_SLICE, null, null, 7, CropSoilRule.CULTIVATED, Blocks.MELON_STEM, StemBlock.AGE),
			new CropInfo(Items.PUMPKIN_SEEDS, Items.PUMPKIN, Items.PUMPKIN, null, null, 7, CropSoilRule.CULTIVATED, Blocks.PUMPKIN_STEM, StemBlock.AGE),
			new CropInfo(Items.TORCHFLOWER_SEEDS, Items.TORCHFLOWER, Items.TORCHFLOWER, null, null, 2, CropSoilRule.CULTIVATED, Blocks.TORCHFLOWER_CROP, TorchflowerCropBlock.AGE),
			new CropInfo(Items.PITCHER_POD, Items.PITCHER_POD, Items.PITCHER_POD, null, null, 4, CropSoilRule.CULTIVATED, Blocks.PITCHER_CROP, PitcherCropBlock.AGE),
			new CropInfo(Items.BAMBOO, Items.BAMBOO, Items.BAMBOO, null, null, 3, CropSoilRule.BAMBOO, Blocks.BAMBOO, BambooStalkBlock.AGE),
			new CropInfo(Items.SUGAR_CANE, Items.SUGAR_CANE, Items.SUGAR_CANE, null, null, 3, CropSoilRule.SUGAR_CANE, Blocks.SUGAR_CANE, null),
			new CropInfo(Items.CACTUS, Items.CACTUS, Items.CACTUS, null, null, 3, CropSoilRule.CACTUS, Blocks.CACTUS, null),
			new CropInfo(Items.BROWN_MUSHROOM, Items.BROWN_MUSHROOM, Items.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM, null, 1, CropSoilRule.SOUL, Blocks.BROWN_MUSHROOM, null),
			new CropInfo(Items.RED_MUSHROOM, Items.RED_MUSHROOM, Items.RED_MUSHROOM, Blocks.RED_MUSHROOM, null, 1, CropSoilRule.SOUL, Blocks.RED_MUSHROOM, null),
			new CropInfo(Items.SWEET_BERRIES, Items.SWEET_BERRIES, Items.SWEET_BERRIES, null, null, 3, CropSoilRule.BERRIES, Blocks.SWEET_BERRY_BUSH, SweetBerryBushBlock.AGE),
			new CropInfo(Items.NETHER_WART, Items.NETHER_WART, Items.NETHER_WART, Blocks.NETHER_WART, NetherWartBlock.AGE, 3, CropSoilRule.SOUL, Blocks.NETHER_WART, NetherWartBlock.AGE)
		);
		private static final List<CropInfo> ENCHANTED_CROPS = List.of(
			new CropInfo(Items.GOLDEN_CARROT, Items.GOLDEN_CARROT, Items.GOLDEN_CARROT, null, null, 3, CropSoilRule.CULTIVATED, null, null),
			new CropInfo(Items.GLISTERING_MELON_SLICE, Items.GLISTERING_MELON_SLICE, Items.GLISTERING_MELON_SLICE, null, null, 7, CropSoilRule.CULTIVATED, null, null)
		);

		private boolean acceptsSoil(ItemStack soil, boolean tilled) {
			if (soil.isEmpty()) {
				return false;
			}
			Block block = Block.byItem(soil.getItem());
			return switch (soilRule) {
				case CULTIVATED -> tilled && isDirtBase(block);
				case BAMBOO -> block == Blocks.DIRT || block == Blocks.GRASS_BLOCK;
				case SUGAR_CANE -> isSand(block) || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK;
				case CACTUS -> isSand(block);
				case SOUL -> block == Blocks.NETHERRACK || block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL;
				case BERRIES -> block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL;
			};
		}

		private boolean requiresWater() {
			return soilRule != CropSoilRule.BAMBOO && soilRule != CropSoilRule.CACTUS && soilRule != CropSoilRule.SOUL;
		}

		private BlockState matureState() {
			if (lootBlock == null) {
				return Blocks.AIR.defaultBlockState();
			}
			BlockState state = lootBlock.defaultBlockState();
			if (ageProperty != null && state.hasProperty(ageProperty)) {
				return state.setValue(ageProperty, maxAge);
			}
			return state;
		}

		private BlockState renderedState(int growthAge) {
			if (plantingItem == Items.MELON_SEEDS && growthAge >= 2) {
				return Blocks.ATTACHED_MELON_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.NORTH);
			}
			if (plantingItem == Items.PUMPKIN_SEEDS && growthAge >= 2) {
				return Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.NORTH);
			}
			if (renderBlock == null) {
				return null;
			}
			BlockState state = renderBlock.defaultBlockState();
			if (renderAgeProperty != null && state.hasProperty(renderAgeProperty)) {
				int maximumRenderAge = renderAgeProperty.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
				state = state.setValue(renderAgeProperty, Math.min(growthAge, maximumRenderAge));
			}
			return state;
		}

		private BlockState renderedFruitState(int growthAge) {
			if (growthAge < maxAge) {
				return null;
			}
			if (plantingItem == Items.MELON_SEEDS) {
				return Blocks.MELON.defaultBlockState();
			}
			if (plantingItem == Items.PUMPKIN_SEEDS) {
				return Blocks.PUMPKIN.defaultBlockState();
			}
			return null;
		}
	}
}
