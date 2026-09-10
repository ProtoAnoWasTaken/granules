package com.puppy.granules.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneResistorBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty INPUT_POWERED = BooleanProperty.create("input_powered");
	public static final IntegerProperty PULSE_TICKS = IntegerProperty.create("pulse_ticks", 0, 2);
	private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

	public RedstoneResistorBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(POWERED, false)
				.setValue(INPUT_POWERED, false)
				.setValue(PULSE_TICKS, 0)
		);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, INPUT_POWERED, PULSE_TICKS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportRigidBlock(level, pos.below());
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess tickAccess,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random
	) {
		if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(state.getBlock())) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void neighborChanged(
		BlockState state,
		Level level,
		BlockPos pos,
		Block neighborBlock,
		Orientation orientation,
		boolean movedByPiston
	) {
		if (!level.isClientSide()) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boolean inputPowered = getInputSignal(level, pos, state) > 0;
		boolean previousInputPowered = state.getValue(INPUT_POWERED);
		if (inputPowered != previousInputPowered) {
			BlockState pulseState = state
				.setValue(INPUT_POWERED, inputPowered)
				.setValue(POWERED, true)
				.setValue(PULSE_TICKS, 2);
			level.setBlock(pos, pulseState, Block.UPDATE_CLIENTS);
			level.updateNeighborsAt(pos, this);
			level.scheduleTick(pos, this, 1);
			return;
		}
		int pulseTicks = state.getValue(PULSE_TICKS);
		if (pulseTicks > 1) {
			BlockState activeState = state.setValue(PULSE_TICKS, pulseTicks - 1);
			level.setBlock(pos, activeState, Block.UPDATE_CLIENTS);
			level.scheduleTick(pos, this, 1);
			return;
		}
		if (state.getValue(POWERED)) {
			BlockState inactiveState = state
				.setValue(POWERED, false)
				.setValue(PULSE_TICKS, 0);
			level.setBlock(pos, inactiveState, Block.UPDATE_CLIENTS);
			level.updateNeighborsAt(pos, this);
		}
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return getOutputSignal(state, direction);
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return getOutputSignal(state, direction);
	}

	private static int getInputSignal(Level level, BlockPos pos, BlockState state) {
		Direction inputDirection = state.getValue(FACING).getOpposite();
		return level.getSignal(pos.relative(inputDirection), inputDirection);
	}

	private static int getOutputSignal(BlockState state, Direction direction) {
		Direction inputDirection = state.getValue(FACING).getOpposite();
		if (!state.getValue(POWERED) || direction != inputDirection) {
			return 0;
		}
		return 15;
	}
}
