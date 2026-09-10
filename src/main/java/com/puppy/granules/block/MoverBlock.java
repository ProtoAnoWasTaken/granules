package com.puppy.granules.block;

import com.mojang.serialization.MapCodec;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.entity.MoverEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoverBlock extends FallingBlock {
	public static final MapCodec<MoverBlock> CODEC = simpleCodec(MoverBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty RIDER_DRIVEN = BooleanProperty.create("rider_driven");

	public MoverBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(ACTIVE, false)
				.setValue(RIDER_DRIVEN, false)
		);
	}

	@Override
	protected MapCodec<? extends FallingBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE, RIDER_DRIVEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState();
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this) && !level.isClientSide()) {
			level.scheduleTick(pos, this, getDelayAfterPlace());
		}
	}

	@Override
	protected BlockState updateShape(
		BlockState state,
		LevelReader level,
		ScheduledTickAccess scheduledTickAccess,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		RandomSource random
	) {
		scheduledTickAccess.scheduleTick(pos, this, getDelayAfterPlace());
		return state;
	}

	@Override
	public int getDustColor(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
		return 0xA8A8A8;
	}

	@Override
	protected void falling(FallingBlockEntity fallingBlock) {
		fallingBlock.setHurtsEntities(2.0F, 40);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!hasRestingSupport(level, pos)) {
			beginFalling(level, pos, state);
		}
	}

	public static boolean routeFromJunction(ServerLevel level, BlockPos moverPos, BlockPos junctionPos, Direction direction) {
		BlockState state = level.getBlockState(moverPos);
		if (!state.is(GranulesMod.MOVER)) {
			return false;
		}
		MoverEntity mover = new MoverEntity(level, moverPos, direction);
		if (!mover.startJunctionRoute(junctionPos, direction)) {
			return false;
		}
		spawnLinkedMoverCluster(level, moverPos, mover);
		return true;
	}

	public static BlockPos findNearestJunctionAlignedMover(LevelReader level, BlockPos moverPos, BlockPos junctionPos) {
		BlockPos nearestMoverPos = moverPos;
		int nearestDistance = Integer.MAX_VALUE;
		BlockState junctionState = level.getBlockState(junctionPos);
		for (BlockPos linkedMoverPos : findLinkedMoverPositions(level, moverPos)) {
			for (Direction contactDirection : Direction.values()) {
				BlockPos contactPos = junctionPos.relative(contactDirection);
				if (!isValidJunctionContact(junctionState, junctionPos, contactPos) || !isAxisAligned(linkedMoverPos, contactPos)) {
					continue;
				}
				int distance = Math.abs(linkedMoverPos.getX() - contactPos.getX())
					+ Math.abs(linkedMoverPos.getY() - contactPos.getY())
					+ Math.abs(linkedMoverPos.getZ() - contactPos.getZ());
				if (distance < nearestDistance) {
					nearestMoverPos = linkedMoverPos;
					nearestDistance = distance;
				}
			}
		}
		return nearestMoverPos;
	}

	private static boolean isAxisAligned(BlockPos firstPos, BlockPos secondPos) {
		int differingAxes = 0;
		if (firstPos.getX() != secondPos.getX()) {
			differingAxes++;
		}
		if (firstPos.getY() != secondPos.getY()) {
			differingAxes++;
		}
		if (firstPos.getZ() != secondPos.getZ()) {
			differingAxes++;
		}
		return differingAxes <= 1;
	}

	public static boolean summonToJunction(ServerLevel level, BlockPos moverPos, BlockPos junctionPos) {
		return summonToJunction(level, moverPos, junctionPos, List.of());
	}

	public static boolean summonToJunction(ServerLevel level, BlockPos moverPos, BlockPos junctionPos, List<BlockPos> routeContacts) {
		return summonToJunction(level, moverPos, junctionPos, routeContacts, List.of());
	}

	public static boolean summonToJunction(ServerLevel level, BlockPos moverPos, BlockPos junctionPos, List<BlockPos> routeContacts, List<BlockPos> routeFlightAnchors) {
		BlockState state = level.getBlockState(moverPos);
		if (!state.is(GranulesMod.MOVER)) {
			return false;
		}
		MoverEntity mover = new MoverEntity(level, moverPos, Direction.NORTH);
		if (!mover.startRouteTo(junctionPos, routeContacts, routeFlightAnchors)) {
			return false;
		}
		spawnLinkedMoverCluster(level, moverPos, mover);
		return true;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!level.getBlockState(pos).is(GranulesMod.MOVER)) {
			return InteractionResult.PASS;
		}
		Direction direction = movementDirection(player);
		MoverEntity mover = new MoverEntity((ServerLevel) level, pos, direction);
		spawnLinkedMoverCluster((ServerLevel) level, pos, mover);
		player.startRiding(mover, true, true);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static Direction movementDirection(Player player) {
		Vec3 look = player.getLookAngle();
		Direction direction;
		if (Math.abs(look.y) > Math.max(Math.abs(look.x), Math.abs(look.z))) {
			direction = look.y > 0.0D ? Direction.UP : Direction.DOWN;
		} else if (Math.abs(look.x) > Math.abs(look.z)) {
			direction = look.x > 0.0D ? Direction.EAST : Direction.WEST;
		} else {
			direction = look.z > 0.0D ? Direction.SOUTH : Direction.NORTH;
		}
		return direction;
	}

	private void beginFalling(ServerLevel level, BlockPos pos, BlockState state) {
		FallingBlockEntity fallingBlock = FallingBlockEntity.fall(
			level,
			pos,
			state.setValue(ACTIVE, false).setValue(RIDER_DRIVEN, false)
		);
		falling(fallingBlock);
	}

	public static boolean hasRestingSupport(LevelReader level, BlockPos pos) {
		Deque<BlockPos> pendingPositions = new ArrayDeque<>();
		Set<BlockPos> checkedPositions = new HashSet<>();
		pendingPositions.add(pos);
		while (!pendingPositions.isEmpty()) {
			BlockPos currentPos = pendingPositions.removeFirst();
			if (!checkedPositions.add(currentPos)) {
				continue;
			}
			for (Direction direction : Direction.values()) {
				BlockPos adjacentPos = currentPos.relative(direction);
				BlockState adjacentState = level.getBlockState(adjacentPos);
				if (direction == Direction.DOWN && !FallingBlock.isFree(adjacentState)) {
					return true;
				}
				if (direction.getAxis().isHorizontal() && adjacentState.is(GranulesMod.MOVER)) {
					pendingPositions.addLast(adjacentPos);
					continue;
				}
				if (isAttachmentBlock(adjacentState)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void spawnLinkedMoverCluster(ServerLevel level, BlockPos leaderPos, MoverEntity leader) {
		List<BlockPos> linkedPositions = findLinkedMoverPositions(level, leaderPos);
		for (BlockPos linkedPos : linkedPositions) {
			level.removeBlock(linkedPos, false);
		}
		level.addFreshEntity(leader);
		for (BlockPos linkedPos : linkedPositions) {
			if (linkedPos.equals(leaderPos)) {
				continue;
			}
			MoverEntity follower = new MoverEntity(level, linkedPos, Direction.NORTH);
			follower.follow(leader);
			level.addFreshEntity(follower);
			leader.addLinkedFollower(follower);
		}
		leader.alignRouteToNearestLinkedContact(level);
	}

	private static List<BlockPos> findLinkedMoverPositions(LevelReader level, BlockPos startPos) {
		Deque<BlockPos> pendingPositions = new ArrayDeque<>();
		Set<BlockPos> checkedPositions = new HashSet<>();
		List<BlockPos> linkedPositions = new ArrayList<>();
		pendingPositions.add(startPos);
		while (!pendingPositions.isEmpty()) {
			BlockPos currentPos = pendingPositions.removeFirst();
			if (!checkedPositions.add(currentPos) || !level.getBlockState(currentPos).is(GranulesMod.MOVER)) {
				continue;
			}
			linkedPositions.add(currentPos);
			for (Direction direction : Direction.values()) {
				if (direction.getAxis().isHorizontal()) {
					pendingPositions.addLast(currentPos.relative(direction));
				}
			}
		}
		return linkedPositions;
	}

	public static boolean isAttachmentBlock(BlockState state) {
		return state.is(GranulesMod.MOVER_ADHERENCE_BLOCKS);
	}

	public static boolean isValidJunction(BlockState state) {
		return state.is(GranulesMod.MOVER_JUNCTIONS);
	}

	public static boolean isValidJunctionContact(BlockState state, BlockPos junctionPos, BlockPos moverPos) {
		if (!isValidJunction(state)) {
			return false;
		}
		if (state.is(GranulesMod.JUNCTION)) {
			return JunctionBlock.isMoverContact(state, junctionPos, moverPos);
		}
		return moverPos.equals(junctionPos.relative(getJunctionDirection(state, junctionPos, moverPos)));
	}

	public static Direction getJunctionDirection(BlockState state, BlockPos junctionPos, BlockPos moverPos) {
		if (state.hasProperty(BlockStateProperties.FACING)) {
			return state.getValue(BlockStateProperties.FACING);
		}
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		}
		int xDistance = moverPos.getX() - junctionPos.getX();
		int yDistance = moverPos.getY() - junctionPos.getY();
		int zDistance = moverPos.getZ() - junctionPos.getZ();
		if (Math.abs(yDistance) > Math.max(Math.abs(xDistance), Math.abs(zDistance))) {
			return yDistance > 0 ? Direction.UP : Direction.DOWN;
		}
		if (Math.abs(xDistance) > Math.abs(zDistance)) {
			return xDistance > 0 ? Direction.EAST : Direction.WEST;
		}
		return zDistance > 0 ? Direction.SOUTH : Direction.NORTH;
	}
}
