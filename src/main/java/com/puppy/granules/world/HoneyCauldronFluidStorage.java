package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.mixin.access.BlockApiLookupAccess;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.custom.ApiProviderMap;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HoneyCauldronFluidStorage extends SnapshotParticipant<BlockState> implements SingleSlotStorage<FluidVariant> {
	private final Level level;
	private final BlockPos pos;
	private BlockState lastReleasedSnapshot;

	private HoneyCauldronFluidStorage(Level level, BlockPos pos) {
		this.level = level;
		this.pos = pos.immutable();
	}

	public static void initialize() {
		BlockApiLookupAccess access = (BlockApiLookupAccess) (Object) FluidStorage.SIDED;
		ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> delegate = access.granules$getProviderMap();
		BlockApiLookup.BlockApiProvider<Storage<FluidVariant>, Direction> provider = HoneyCauldronFluidStorage::find;
		BlockApiLookup.BlockApiProvider<Storage<FluidVariant>, Direction> enchantedProvider = (level, pos, state, blockEntity, direction) -> new EnchantedCauldronFluidStorage(level, pos);
		ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> replacement = new HoneyCauldronProviderMap(delegate, provider, enchantedProvider);
		access.granules$setProviderMap(replacement);
	}

	private static Storage<FluidVariant> find(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity, Direction direction) {
		return new HoneyCauldronFluidStorage(level, pos);
	}

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		if (!resource.isOf(GranulesMod.HONEY) || maxAmount < FluidConstants.BOTTLE) {
			return 0L;
		}

		int currentLevel = HoneyCauldrons.getHoneyLevel(this.getState());
		int levelsToInsert = (int) Math.min(maxAmount / FluidConstants.BOTTLE, 3 - currentLevel);
		if (levelsToInsert <= 0) {
			return 0L;
		}

		this.setHoneyLevel(currentLevel + levelsToInsert, transaction);
		return levelsToInsert * FluidConstants.BOTTLE;
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		if (!resource.isOf(GranulesMod.HONEY) || maxAmount < FluidConstants.BOTTLE) {
			return 0L;
		}

		int currentLevel = HoneyCauldrons.getHoneyLevel(this.getState());
		int levelsToExtract = (int) Math.min(maxAmount / FluidConstants.BOTTLE, currentLevel);
		if (levelsToExtract <= 0) {
			return 0L;
		}

		this.setHoneyLevel(currentLevel - levelsToExtract, transaction);
		return levelsToExtract * FluidConstants.BOTTLE;
	}

	@Override
	public boolean isResourceBlank() {
		return HoneyCauldrons.getHoneyLevel(this.getState()) == 0;
	}

	@Override
	public FluidVariant getResource() {
		return this.isResourceBlank() ? FluidVariant.blank() : FluidVariant.of(GranulesMod.HONEY);
	}

	@Override
	public long getAmount() {
		return HoneyCauldrons.getHoneyLevel(this.getState()) * FluidConstants.BOTTLE;
	}

	@Override
	public long getCapacity() {
		return 3L * FluidConstants.BOTTLE;
	}

	@Override
	protected BlockState createSnapshot() {
		return this.getState();
	}

	@Override
	protected void readSnapshot(BlockState snapshot) {
		this.level.setBlock(this.pos, snapshot, 0);
	}

	@Override
	protected void releaseSnapshot(BlockState snapshot) {
		this.lastReleasedSnapshot = snapshot;
	}

	@Override
	protected void onFinalCommit() {
		BlockState currentState = this.getState();
		if (this.lastReleasedSnapshot != currentState) {
			this.level.setBlock(this.pos, this.lastReleasedSnapshot, 0);
			this.level.setBlockAndUpdate(this.pos, currentState);
		}
	}

	private BlockState getState() {
		return this.level.getBlockState(this.pos);
	}

	private void setHoneyLevel(int honeyLevel, TransactionContext transaction) {
		this.updateSnapshots(transaction);
		BlockState newState = HoneyCauldrons.withHoneyLevel(this.getState(), honeyLevel);
		this.level.setBlock(this.pos, newState, 0);
	}

	private static class HoneyCauldronProviderMap implements ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> {
		private final ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> delegate;
		private final BlockApiLookup.BlockApiProvider<Storage<FluidVariant>, Direction> provider;
		private final BlockApiLookup.BlockApiProvider<Storage<FluidVariant>, Direction> enchantedProvider;

		private HoneyCauldronProviderMap(
			ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> delegate,
			BlockApiLookup.BlockApiProvider<Storage<FluidVariant>, Direction> provider,
			BlockApiLookup.BlockApiProvider<Storage<FluidVariant>, Direction> enchantedProvider
		) {
			this.delegate = delegate;
			this.provider = provider;
			this.enchantedProvider = enchantedProvider;
		}

		@Override
		public BlockApiLookup.BlockApiProvider<?, ?> get(Block block) {
			if (block == Blocks.CAULDRON) {
				return provider;
			}
			if (block == GranulesMod.ENCHANTED_CAULDRON) {
				return enchantedProvider;
			}
			return delegate.get(block);
		}

		@Override
		public BlockApiLookup.BlockApiProvider<?, ?> putIfAbsent(Block block, BlockApiLookup.BlockApiProvider<?, ?> provider) {
			if (block == Blocks.CAULDRON) {
				return this.provider;
			}
			if (block == GranulesMod.ENCHANTED_CAULDRON) {
				return enchantedProvider;
			}
			return delegate.putIfAbsent(block, provider);
		}
	}
}
