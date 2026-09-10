package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class EnchantedCauldronFluidStorage extends SnapshotParticipant<BlockState> implements SingleSlotStorage<FluidVariant> {
	private final Level level;
	private final BlockPos pos;
	private BlockState lastReleasedSnapshot;

	EnchantedCauldronFluidStorage(Level level, BlockPos pos) {
		this.level = level;
		this.pos = pos.immutable();
	}

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		EnchantedCauldronProperties.Content content = contentFor(resource);
		if (content == EnchantedCauldronProperties.Content.EMPTY) {
			return 0L;
		}
		long unit = unitFor(content);
		int maximum = maximumFor(content);
		if (maxAmount < unit || !canStore(content)) {
			return 0L;
		}
		int insertedLevels = (int) Math.min(maxAmount / unit, maximum - getLevel());
		if (insertedLevels <= 0) {
			return 0L;
		}
		setContent(content, getLevel() + insertedLevels, transaction);
		return insertedLevels * unit;
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		EnchantedCauldronProperties.Content content = getContent();
		if (content == EnchantedCauldronProperties.Content.EMPTY || content != contentFor(resource)) {
			return 0L;
		}
		long unit = unitFor(content);
		if (maxAmount < unit) {
			return 0L;
		}
		int extractedLevels = (int) Math.min(maxAmount / unit, getLevel());
		if (extractedLevels <= 0) {
			return 0L;
		}
		setContent(content, getLevel() - extractedLevels, transaction);
		return extractedLevels * unit;
	}

	@Override
	public boolean isResourceBlank() {
		return getResource().isBlank();
	}

	@Override
	public FluidVariant getResource() {
		return switch (getContent()) {
			case WATER -> FluidVariant.of(Fluids.WATER);
			case HONEY -> FluidVariant.of(GranulesMod.HONEY);
			case LAVA -> FluidVariant.of(Fluids.LAVA);
			case EMPTY, POWDER_SNOW -> FluidVariant.blank();
		};
	}

	@Override
	public long getAmount() {
		return getLevel() * unitFor(getContent());
	}

	@Override
	public long getCapacity() {
		return maximumFor(getContent()) * unitFor(getContent());
	}

	@Override
	protected BlockState createSnapshot() {
		return getState();
	}

	@Override
	protected void readSnapshot(BlockState snapshot) {
		level.setBlock(pos, snapshot, 0);
	}

	@Override
	protected void releaseSnapshot(BlockState snapshot) {
		lastReleasedSnapshot = snapshot;
	}

	@Override
	protected void onFinalCommit() {
		BlockState currentState = getState();
		if (lastReleasedSnapshot != currentState) {
			level.setBlock(pos, lastReleasedSnapshot, 0);
			level.setBlockAndUpdate(pos, currentState);
		}
	}

	private boolean canStore(EnchantedCauldronProperties.Content content) {
		EnchantedCauldronProperties.Content currentContent = getContent();
		return currentContent == EnchantedCauldronProperties.Content.EMPTY || currentContent == content;
	}

	private EnchantedCauldronProperties.Content contentFor(FluidVariant resource) {
		if (resource.isOf(Fluids.WATER)) {
			return EnchantedCauldronProperties.Content.WATER;
		}
		if (resource.isOf(GranulesMod.HONEY)) {
			return EnchantedCauldronProperties.Content.HONEY;
		}
		if (resource.isOf(Fluids.LAVA)) {
			return EnchantedCauldronProperties.Content.LAVA;
		}
		return EnchantedCauldronProperties.Content.EMPTY;
	}

	private long unitFor(EnchantedCauldronProperties.Content content) {
		return content == EnchantedCauldronProperties.Content.LAVA ? FluidConstants.BUCKET : FluidConstants.BOTTLE;
	}

	private int maximumFor(EnchantedCauldronProperties.Content content) {
		return content == EnchantedCauldronProperties.Content.LAVA ? 3 : 9;
	}

	private EnchantedCauldronProperties.Content getContent() {
		return getState().getValue(EnchantedCauldronProperties.CONTENT);
	}

	private int getLevel() {
		return getState().getValue(EnchantedCauldronProperties.LEVEL);
	}

	private BlockState getState() {
		return level.getBlockState(pos);
	}

	private void setContent(EnchantedCauldronProperties.Content content, int level, TransactionContext transaction) {
		updateSnapshots(transaction);
		EnchantedCauldronProperties.Content newContent = level == 0 ? EnchantedCauldronProperties.Content.EMPTY : content;
		BlockState state = getState()
			.setValue(EnchantedCauldronProperties.CONTENT, newContent)
			.setValue(EnchantedCauldronProperties.LEVEL, level);
		this.level.setBlock(pos, state, 0);
	}
}
