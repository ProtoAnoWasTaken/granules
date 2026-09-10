package com.puppy.granules.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.tags.FluidTags;

public class HoneyLiquidBlock extends LiquidBlock {
	public HoneyLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
		super(fluid, properties);
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 3;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		this.resolveFluidContact(state, level, pos);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
		super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
		this.resolveFluidContact(state, level, pos);
	}

	private void resolveFluidContact(BlockState state, Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return;
		}

		boolean touchesWater = false;
		boolean touchesLava = false;
		for (Direction direction : Direction.values()) {
			FluidState adjacentFluid = level.getFluidState(pos.relative(direction));
			touchesWater |= adjacentFluid.is(FluidTags.WATER);
			touchesLava |= adjacentFluid.is(FluidTags.LAVA);
		}

		if (!touchesWater && !touchesLava) {
			return;
		}

		if (!state.getFluidState().isSource()) {
			level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			return;
		}

		BlockState replacement = touchesLava ? Blocks.HONEYCOMB_BLOCK.defaultBlockState() : Blocks.HONEY_BLOCK.defaultBlockState();
		level.setBlockAndUpdate(pos, replacement);
	}
}
