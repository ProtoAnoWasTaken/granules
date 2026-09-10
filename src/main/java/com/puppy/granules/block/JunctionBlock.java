package com.puppy.granules.block;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.MoverConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class JunctionBlock extends Block {
	private static final Logger LOGGER = LoggerFactory.getLogger("Granules Junction");
	private static final int JUNCTION_ROUTE_RANGE = 24;
	private static final int LODESTONE_ROUTE_RANGE = 36;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty SUMMONING = BooleanProperty.create("summoning");

	public JunctionBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(POWERED, false)
				.setValue(SUMMONING, false)
		);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, SUMMONING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(POWERED, false)
			.setValue(SUMMONING, false);
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
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(this)) {
			updateState(level, pos, state);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide()) {
			updateState(level, pos, state);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		updateState(level, pos, state);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		if (state.getValue(POWERED) && direction == state.getValue(FACING).getOpposite()) {
			return 15;
		}
		return 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return getSignal(state, level, pos, direction);
	}

	public static boolean isMoverContact(BlockState state, BlockPos junctionPos, BlockPos moverPos) {
		return moverPos.equals(junctionPos.north())
			|| moverPos.equals(junctionPos.south())
			|| moverPos.equals(junctionPos.east())
			|| moverPos.equals(junctionPos.west())
			|| moverPos.equals(junctionPos.above());
	}

	private void updateState(Level level, BlockPos pos, BlockState state) {
		List<BlockPos> moverContacts = findMoverContacts(level, pos, state);
		boolean moverContact = !moverContacts.isEmpty();
		boolean summoningInput = hasSummoningInput(level, pos, state);
		BlockState updatedState = state.setValue(POWERED, moverContact).setValue(SUMMONING, summoningInput);
		if (!updatedState.equals(state)) {
			level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS);
			level.updateNeighborsAt(pos, this);
		}
		if (!summoningInput || state.getValue(SUMMONING) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		LOGGER.info("Recognized redstone pulse at Junction {} with contacting Movers {}.", pos, moverContacts);
		if (moverContact) {
			LOGGER.info("Dispatching contacting Movers from Junction {}.", pos);
			BlockPos pivotMoverPos = selectDispatchPivot(pos, state, moverContacts);
			if (!MoverBlock.routeFromJunction(serverLevel, pivotMoverPos, pos, state.getValue(FACING))) {
				LOGGER.warn("Mover at {} has no forward route away from Junction {}.", pivotMoverPos, pos);
			}
			return;
		}
		boolean rangBell = ringNearbyBells(serverLevel, pos);
		LOGGER.info("Bell call from Junction {} was {}.", pos, rangBell ? "attempted" : "skipped because no Bell was in range");
		if (rangBell) {
			summonMover(serverLevel, pos);
		}
	}

	private boolean hasSummoningInput(Level level, BlockPos pos, BlockState state) {
		List<BlockPos> moverContacts = findMoverContacts(level, pos, state);
		for (Direction direction : Direction.values()) {
			BlockPos inputPos = pos.relative(direction);
			if (moverContacts.contains(inputPos)) {
				continue;
			}
			if (MoverBlock.isValidJunction(level.getBlockState(inputPos))) {
				continue;
			}
			if (level.getSignal(inputPos, direction.getOpposite()) > 0) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos selectDispatchPivot(BlockPos junctionPos, BlockState state, List<BlockPos> moverContacts) {
		BlockPos forwardAlignedContact = junctionPos.relative(state.getValue(FACING).getOpposite());
		if (moverContacts.contains(forwardAlignedContact)) {
			return forwardAlignedContact;
		}
		return moverContacts.getFirst();
	}

	private boolean ringNearbyBells(ServerLevel level, BlockPos pos) {
		boolean rangBell = false;
		int bellSummonRadius = MoverConfig.get().bellSummonRadius();
		for (int xOffset = -bellSummonRadius; xOffset <= bellSummonRadius; xOffset++) {
			for (int yOffset = -bellSummonRadius; yOffset <= bellSummonRadius; yOffset++) {
				for (int zOffset = -bellSummonRadius; zOffset <= bellSummonRadius; zOffset++) {
					if (xOffset * xOffset + yOffset * yOffset + zOffset * zOffset > bellSummonRadius * bellSummonRadius) {
						continue;
					}
					BlockPos bellPos = pos.offset(xOffset, yOffset, zOffset);
					BlockState bellState = level.getBlockState(bellPos);
					if (bellState.getBlock() instanceof net.minecraft.world.level.block.BellBlock bellBlock) {
						bellBlock.attemptToRing(level, bellPos, null);
						rangBell = true;
					}
				}
			}
		}
		return rangBell;
	}

	private boolean summonMover(ServerLevel level, BlockPos junctionPos) {
		int moverPullDistance = MoverConfig.get().moverPullDistance();
		long searchStart = System.nanoTime();
		LOGGER.info("Searching for a Mover through connected routes to Junction {} with a {} block budget.", junctionPos, moverPullDistance);
		List<CallRoute> candidates = this.findCallCandidates(level, junctionPos, moverPullDistance);
		long searchDurationMillis = (System.nanoTime() - searchStart) / 1_000_000L;
		LOGGER.info("Route search for Junction {} found {} Mover candidates in {} ms.", junctionPos, candidates.size(), searchDurationMillis);
		if (candidates.isEmpty()) {
			LOGGER.warn("No stationary Mover candidates are aligned with Junction {}.", junctionPos);
			return false;
		}
		List<BlockPos> attemptedCandidates = candidates.stream().map(CallRoute::moverPos).toList();
		candidates.sort(Comparator.comparingInt(CallRoute::travelDistance));
		while (!candidates.isEmpty()) {
			CallRoute candidate = candidates.removeFirst();
			LOGGER.info("Trying Mover {} through {} legal route blocks and {} Junction contacts.", candidate.moverPos(), candidate.travelDistance(), candidate.contacts().size());
			if (MoverBlock.summonToJunction(level, candidate.moverPos(), junctionPos, candidate.contacts(), candidate.flightAnchors())) {
				return true;
			}
		}
		LOGGER.warn("Mover candidates {} cannot route to Junction {}.", attemptedCandidates, junctionPos);
		return false;
	}

	private List<CallRoute> findCallCandidates(ServerLevel level, BlockPos destinationJunctionPos, int maximumTravelDistance) {
		Map<BlockPos, CallRoute> candidates = new HashMap<>();
		PriorityQueue<RouteSearchNode> pendingContacts = new PriorityQueue<>(Comparator.comparingInt(RouteSearchNode::travelDistance));
		Map<BlockPos, Integer> shortestContactDistances = new HashMap<>();
		BlockState destinationState = level.getBlockState(destinationJunctionPos);
		for (Direction contactDirection : Direction.values()) {
			BlockPos contactPos = destinationJunctionPos.relative(contactDirection);
			BlockState contactState = level.getBlockState(contactPos);
			if (MoverBlock.isValidJunctionContact(destinationState, destinationJunctionPos, contactPos)
				&& isTraversableCallPath(level, contactPos, contactState)) {
				List<FlightAnchor> flightAnchors = new ArrayList<>();
				flightAnchors.add(new FlightAnchor(destinationJunctionPos, JUNCTION_ROUTE_RANGE));
				this.addContactFlightAnchors(level, contactPos, flightAnchors);
				pendingContacts.add(new RouteSearchNode(contactPos, 0, List.of(contactPos), List.copyOf(flightAnchors)));
				shortestContactDistances.put(contactPos, 0);
			}
		}
		while (!pendingContacts.isEmpty()) {
			RouteSearchNode node = pendingContacts.remove();
			Integer shortestDistance = shortestContactDistances.get(node.pos());
			if (shortestDistance == null || node.travelDistance() != shortestDistance) {
				continue;
			}
			for (Direction routeDirection : Direction.values()) {
				List<FlightAnchor> segmentAnchors = new ArrayList<>(node.flightAnchors());
				for (int segmentDistance = 1; node.travelDistance() + segmentDistance <= maximumTravelDistance; segmentDistance++) {
					BlockPos routePos = node.pos().relative(routeDirection, segmentDistance);
					BlockState routeState = level.getBlockState(routePos);
					this.addContactFlightAnchors(level, routePos, segmentAnchors);
					if (!isWithinCallFlightRoom(routePos, segmentAnchors)) {
						break;
					}
					int routeDistance = node.travelDistance() + segmentDistance;
					if (routeState.is(GranulesMod.MOVER)) {
						BlockPos pivotMoverPos = MoverBlock.findNearestJunctionAlignedMover(level, routePos, destinationJunctionPos);
						if (routePos.equals(pivotMoverPos)) {
							CallRoute candidate = new CallRoute(routePos, node.contacts(), segmentAnchors.stream().map(FlightAnchor::pos).toList(), routeDistance);
							CallRoute currentCandidate = candidates.get(routePos);
							if (currentCandidate == null || candidate.travelDistance() < currentCandidate.travelDistance()) {
								candidates.put(routePos, candidate);
							}
						}
						break;
					}
					if (!isTraversableCallPath(level, routePos, routeState)) {
						break;
					}
					if (!isConnectedJunctionContact(level, routePos)) {
						continue;
					}
					Integer knownDistance = shortestContactDistances.get(routePos);
					if (knownDistance == null || routeDistance < knownDistance) {
						List<BlockPos> contacts = new ArrayList<>(node.contacts());
						contacts.add(routePos);
						shortestContactDistances.put(routePos, routeDistance);
						pendingContacts.add(new RouteSearchNode(routePos, routeDistance, List.copyOf(contacts), List.copyOf(segmentAnchors)));
					}
					break;
				}
			}
		}
		return new ArrayList<>(candidates.values());
	}

	private static boolean isTraversableCallPath(ServerLevel level, BlockPos pos, BlockState state) {
		return state.isAir()
			|| !state.getFluidState().isEmpty()
			|| state.getDestroySpeed(level, pos) <= 0.0F;
	}

	private void addContactFlightAnchors(ServerLevel level, BlockPos pos, List<FlightAnchor> flightAnchors) {
		for (Direction direction : Direction.values()) {
			BlockPos anchorPos = pos.relative(direction);
			BlockState anchorState = level.getBlockState(anchorPos);
			int range = anchorState.is(Blocks.LODESTONE) ? LODESTONE_ROUTE_RANGE : MoverBlock.isValidJunctionContact(anchorState, anchorPos, pos) ? JUNCTION_ROUTE_RANGE : 0;
			if (range > 0 && flightAnchors.stream().noneMatch(anchor -> anchor.pos().equals(anchorPos))) {
				flightAnchors.add(new FlightAnchor(anchorPos, range));
			}
		}
	}

	private static boolean isWithinCallFlightRoom(BlockPos pos, List<FlightAnchor> flightAnchors) {
		for (FlightAnchor anchor : flightAnchors) {
			if (Math.abs(pos.getX() - anchor.pos().getX()) <= anchor.range()
				&& Math.abs(pos.getY() - anchor.pos().getY()) <= anchor.range()
				&& Math.abs(pos.getZ() - anchor.pos().getZ()) <= anchor.range()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isConnectedJunctionContact(ServerLevel level, BlockPos contactPos) {
		for (Direction direction : Direction.values()) {
			BlockPos junctionPos = contactPos.relative(direction);
			BlockState junctionState = level.getBlockState(junctionPos);
			if (MoverBlock.isValidJunctionContact(junctionState, junctionPos, contactPos)) {
				return true;
			}
		}
		return false;
	}

	private List<BlockPos> findMoverContacts(Level level, BlockPos pos, BlockState state) {
		List<BlockPos> contacts = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			BlockPos sidePos = pos.relative(direction);
			if (isValidMoverContact(level, pos, state, sidePos)) {
				contacts.add(sidePos);
			}
		}
		return contacts;
	}

	private boolean isValidMoverContact(Level level, BlockPos junctionPos, BlockState junctionState, BlockPos moverPos) {
		return level.getBlockState(moverPos).is(GranulesMod.MOVER)
			&& MoverBlock.isValidJunctionContact(junctionState, junctionPos, moverPos);
	}

	private record RouteSearchNode(BlockPos pos, int travelDistance, List<BlockPos> contacts, List<FlightAnchor> flightAnchors) {
	}

	private record FlightAnchor(BlockPos pos, int range) {
	}

	private record CallRoute(BlockPos moverPos, List<BlockPos> contacts, List<BlockPos> flightAnchors, int travelDistance) {
	}
}
