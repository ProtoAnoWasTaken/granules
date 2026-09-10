package com.puppy.granules.entity;

import com.puppy.granules.advancement.GranulesAdvancements;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.block.MoverBlock;
import com.puppy.granules.config.MoverConfig;
import com.puppy.granules.world.HardHatProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.Projectile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public class MoverEntity extends Entity {
	private static final Logger LOGGER = LoggerFactory.getLogger("Granules Mover Route");
	private static final EntityDataAccessor<Byte> DIRECTION = SynchedEntityData.defineId(MoverEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(MoverEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<String> ROUTE_PREVIEW_SEGMENTS = SynchedEntityData.defineId(MoverEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> ROUTE_PREVIEWING = SynchedEntityData.defineId(MoverEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double ACCELERATION = 0.012D;
	private static final double MAX_SPEED = 1.4D;
	private static final double SEAT_HEIGHT = 0.4D;
	private static final int ROUTE_PREVIEW_DURATION = 60;
	private static final int JUNCTION_ROUTE_RANGE = 24;
	private static final int LODESTONE_ROUTE_RANGE = 36;
	private static final int MAX_ROUTE_SEARCH_DISTANCE = 256;
	private static final int MAX_GRID_ROUTE_NODES = 32768;
	private static long nextPassengerOrder;
	private Direction direction = Direction.NORTH;
	private BlockPos anchorPos = BlockPos.ZERO;
	private int anchorRange;
	private RouteTarget target;
	private final List<QueuedRoute> queuedRoutes = new ArrayList<>();
	private List<FlightAnchor> routeFlightAnchors = List.of();
	private List<FlightAnchor> previewFlightAnchors = List.of();
	private BlockPos previewOrigin = BlockPos.ZERO;
	private BlockPos previewAnchorPos = BlockPos.ZERO;
	private int previewAnchorRange;
	private Direction previewDirection;
	private Component routeSignPreview;
	private UUID linkLeaderId;
	private Vec3 linkOffset = Vec3.ZERO;
	private final List<UUID> linkedFollowerIds = new ArrayList<>();
	private long boardingOrder = Long.MAX_VALUE;
	private boolean autonomous;
	private boolean forwardWasPressed;
	private boolean backwardWasPressed;
	private int routePreviewTicks;

	public MoverEntity(EntityType<? extends MoverEntity> type, Level level) {
		super(type, level);
		this.blocksBuilding = true;
		this.setNoGravity(true);
	}

	public MoverEntity(ServerLevel level, BlockPos pos, Direction direction) {
		this(GranulesMod.MOVER_ENTITY, level);
		this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		this.xo = this.getX();
		this.yo = this.getY();
		this.zo = this.getZ();
		this.setDirection(direction);
		this.updateAnchor();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data) {
		data.define(DIRECTION, (byte) Direction.NORTH.ordinal());
		data.define(SPEED, 0.0F);
		data.define(ROUTE_PREVIEW_SEGMENTS, "");
		data.define(ROUTE_PREVIEWING, false);
	}

	@Override
	public void tick() {
		super.tick();
		this.direction = Direction.values()[Math.floorMod(this.entityData.get(DIRECTION), Direction.values().length)];
		if (this.level().isClientSide()) {
			this.tickClientMovement();
			return;
		}
		if (this.isLinkedFollower()) {
			this.tickLinkedFollower((ServerLevel) this.level());
			return;
		}
		this.tickMovement((ServerLevel) this.level());
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.NONE;
	}

	@Override
	public boolean isPickable() {
		return !this.isRemoved();
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		this.markHurt();
		return false;
	}

	@Override
	public LivingEntity getControllingPassenger() {
		MoverEntity groupLeader = this.getGroupLeader();
		if (groupLeader != null) {
			return groupLeader.findGroupCaptain();
		}
		Entity passenger = this.getFirstPassenger();
		return passenger instanceof Player player ? player : null;
	}

	@Override
	protected boolean isLocalClientAuthoritative() {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty() && passenger instanceof Player;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPosition) {
		if (this.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player.startRiding(this, true, true)) {
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (passenger instanceof Player) {
			this.boardingOrder = nextPassengerOrder;
			nextPassengerOrder++;
		}
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return this.position().add(0.0D, 1.0D, 0.0D);
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return this.position().add(0.0D, SEAT_HEIGHT, 0.0D);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		Vec3 seatPosition = this.position().add(0.0D, SEAT_HEIGHT, 0.0D);
		moveFunction.accept(passenger, seatPosition.x, seatPosition.y, seatPosition.z);
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof Player) {
			this.boardingOrder = Long.MAX_VALUE;
		}
		if (!(this.level() instanceof ServerLevel serverLevel) || this.isRemoved() || this.isLinkedFollower()) {
			return;
		}
		if (this.getControllingPassenger() != null) {
			return;
		}
		this.target = null;
		this.queuedRoutes.clear();
		this.routeFlightAnchors = List.of();
		this.clearRoutePreview();
		this.autonomous = false;
		this.setSpeed(0.0F);
		this.setDeltaMovement(Vec3.ZERO);
		BlockPos restingPos = this.blockPosition();
		if (!serverLevel.getBlockState(restingPos).canBeReplaced()) {
			return;
		}
		serverLevel.setBlock(restingPos, GranulesMod.MOVER.defaultBlockState(), Block.UPDATE_ALL);
		passenger.teleportTo(restingPos.getX() + 0.5D, restingPos.getY() + 1.0D, restingPos.getZ() + 0.5D);
		this.discard();
	}

	@Override
	public boolean shouldBeSaved() {
		return true;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putByte("Direction", (byte) this.direction.ordinal());
		output.putFloat("Speed", this.getMoverSpeed());
		output.putInt("AnchorX", this.anchorPos.getX());
		output.putInt("AnchorY", this.anchorPos.getY());
		output.putInt("AnchorZ", this.anchorPos.getZ());
		output.putInt("AnchorRange", this.anchorRange);
		output.putBoolean("Autonomous", this.autonomous);
		output.putBoolean("LinkedFollower", this.linkLeaderId != null);
		if (this.linkLeaderId != null) {
			output.putString("LinkLeader", this.linkLeaderId.toString());
			output.putDouble("LinkOffsetX", this.linkOffset.x);
			output.putDouble("LinkOffsetY", this.linkOffset.y);
			output.putDouble("LinkOffsetZ", this.linkOffset.z);
		}
		output.putString("LinkedFollowers", linkedFollowersAsString());
		output.putLong("BoardingOrder", this.boardingOrder);
		output.putBoolean("HasTarget", this.target != null);
		if (this.target != null) {
			BlockPos targetJunctionPos = this.target.junctionPos();
			output.putBoolean("TargetHasJunction", targetJunctionPos != null);
			if (targetJunctionPos != null) {
				output.putInt("TargetJunctionX", targetJunctionPos.getX());
				output.putInt("TargetJunctionY", targetJunctionPos.getY());
				output.putInt("TargetJunctionZ", targetJunctionPos.getZ());
			}
			output.putInt("TargetX", this.target.contactPos().getX());
			output.putInt("TargetY", this.target.contactPos().getY());
			output.putInt("TargetZ", this.target.contactPos().getZ());
		}
		output.putString("QueuedRoutes", this.queuedRoutesAsString());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.setDirection(Direction.values()[Math.floorMod(input.getByteOr("Direction", (byte) Direction.NORTH.ordinal()), Direction.values().length)]);
		this.setSpeed(input.getFloatOr("Speed", 0.0F));
		this.anchorPos = new BlockPos(input.getIntOr("AnchorX", 0), input.getIntOr("AnchorY", 0), input.getIntOr("AnchorZ", 0));
		this.anchorRange = input.getIntOr("AnchorRange", 0);
		this.autonomous = input.getBooleanOr("Autonomous", false);
		this.linkLeaderId = null;
		this.linkOffset = Vec3.ZERO;
		if (input.getBooleanOr("LinkedFollower", false)) {
			try {
				this.linkLeaderId = UUID.fromString(input.getStringOr("LinkLeader", ""));
				this.linkOffset = new Vec3(
					input.getDoubleOr("LinkOffsetX", 0.0D),
					input.getDoubleOr("LinkOffsetY", 0.0D),
					input.getDoubleOr("LinkOffsetZ", 0.0D)
				);
			} catch (IllegalArgumentException ignored) {
				this.linkLeaderId = null;
				this.linkOffset = Vec3.ZERO;
			}
		}
		this.linkedFollowerIds.clear();
		readLinkedFollowers(input.getStringOr("LinkedFollowers", ""));
		this.boardingOrder = input.getLongOr("BoardingOrder", Long.MAX_VALUE);
		if (this.boardingOrder != Long.MAX_VALUE) {
			nextPassengerOrder = Math.max(nextPassengerOrder, this.boardingOrder + 1L);
		}
		if (input.getBooleanOr("HasTarget", false)) {
			BlockPos targetJunctionPos = null;
			if (input.getBooleanOr("TargetHasJunction", true)) {
				targetJunctionPos = new BlockPos(
					input.getIntOr("TargetJunctionX", 0),
					input.getIntOr("TargetJunctionY", 0),
					input.getIntOr("TargetJunctionZ", 0)
				);
			}
			this.target = new RouteTarget(
				targetJunctionPos,
				new BlockPos(input.getIntOr("TargetX", 0), input.getIntOr("TargetY", 0), input.getIntOr("TargetZ", 0))
			);
		}
		this.queuedRoutes.clear();
		this.readQueuedRoutes(input.getStringOr("QueuedRoutes", ""));
	}

	public BlockState getBlockState() {
		return GranulesMod.MOVER.defaultBlockState();
	}

	public BlockPos getStartPos() {
		return this.anchorRange > 0 ? this.anchorPos : this.blockPosition();
	}

	public float getMoverSpeed() {
		return this.entityData.get(SPEED);
	}

	public boolean isMovingThroughFluid() {
		return this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER)
			|| this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA);
	}

	public boolean isPreviewingRoute() {
		return this.entityData.get(ROUTE_PREVIEWING);
	}

	public List<Vec3> getRoutePreviewPoints() {
		String storedSegments = this.entityData.get(ROUTE_PREVIEW_SEGMENTS);
		if (storedSegments.isBlank()) {
			return List.of();
		}
		List<Vec3> points = new ArrayList<>();
		for (String storedSegment : storedSegments.split(";")) {
			String[] coordinates = storedSegment.split(",");
			if (coordinates.length != 3) {
				continue;
			}
			try {
				int x = Integer.parseInt(coordinates[0]);
				int y = Integer.parseInt(coordinates[1]);
				int z = Integer.parseInt(coordinates[2]);
				points.add(new Vec3(x + 0.5D, y, z + 0.5D));
			} catch (NumberFormatException ignored) {
			}
		}
		return points;
	}

	public void follow(MoverEntity leader) {
		this.linkLeaderId = leader.getUUID();
		this.linkOffset = this.position().subtract(leader.position());
	}

	public void addLinkedFollower(MoverEntity follower) {
		if (!this.linkedFollowerIds.contains(follower.getUUID())) {
			this.linkedFollowerIds.add(follower.getUUID());
		}
	}

	public void removeLinkedFollower(MoverEntity follower) {
		this.linkedFollowerIds.remove(follower.getUUID());
	}

	public void alignRouteToNearestLinkedContact(ServerLevel level) {
		if (this.target == null || this.target.junctionPos() == null || this.linkedFollowerIds.isEmpty()) {
			return;
		}
		RouteTarget originalTarget = this.target;
		Direction routeDirection = this.directionTo(originalTarget.contactPos());
		if (routeDirection == null) {
			return;
		}
		BlockState junctionState = level.getBlockState(originalTarget.junctionPos());
		List<MoverEntity> linkedMovers = this.getLinkedMovers(level);
		LinkedContactCandidate nearestCandidate = null;
		for (MoverEntity mover : linkedMovers) {
			BlockPos moverOffset = mover.blockPosition().subtract(this.blockPosition());
			for (Direction contactDirection : Direction.values()) {
				BlockPos contactPos = originalTarget.junctionPos().relative(contactDirection);
				if (!MoverBlock.isValidJunctionContact(junctionState, originalTarget.junctionPos(), contactPos)) {
					continue;
				}
				BlockPos leaderTargetPos = contactPos.offset(-moverOffset.getX(), -moverOffset.getY(), -moverOffset.getZ());
				if (this.directionTo(leaderTargetPos) != routeDirection || !this.isGroupClearOfJunction(linkedMovers, leaderTargetPos, originalTarget.junctionPos())) {
					continue;
				}
				int contactDistance = routeDistanceBetween(mover.blockPosition(), contactPos);
				if (nearestCandidate == null || contactDistance < nearestCandidate.contactDistance()) {
					nearestCandidate = new LinkedContactCandidate(leaderTargetPos, contactDistance);
				}
			}
		}
		if (nearestCandidate == null) {
			LOGGER.warn("Mover cluster at {} cannot safely attach to Junction {} without placing a linked Mover inside it.", this.blockPosition(), originalTarget.junctionPos());
			this.target = null;
			this.queuedRoutes.clear();
			this.routeFlightAnchors = List.of();
			this.clearRoutePreview();
			this.setSpeed(0.0F);
			this.setDeltaMovement(Vec3.ZERO);
			return;
		}
		this.target = new RouteTarget(originalTarget.junctionPos(), nearestCandidate.leaderTargetPos());
		this.updateRoutePreviewSegments();
	}

	private boolean isGroupClearOfJunction(List<MoverEntity> linkedMovers, BlockPos leaderTargetPos, BlockPos junctionPos) {
		for (MoverEntity mover : linkedMovers) {
			BlockPos moverOffset = mover.blockPosition().subtract(this.blockPosition());
			if (leaderTargetPos.offset(moverOffset.getX(), moverOffset.getY(), moverOffset.getZ()).equals(junctionPos)) {
				return false;
			}
		}
		return true;
	}

	public boolean startJunctionRoute(BlockPos junctionPos, Direction direction) {
		this.autonomous = false;
		this.queuedRoutes.clear();
		RoutePlan forwardRoute = this.findNearestJunction(direction, junctionPos);
		if (forwardRoute != null) {
			this.applyRoute(forwardRoute, direction);
			return true;
		}
		List<Direction> routeDirections = new ArrayList<>();
		for (Direction candidateDirection : Direction.values()) {
			if (candidateDirection == direction.getOpposite()) {
				continue;
			}
			if (this.findNearestJunction(candidateDirection, junctionPos) != null) {
				routeDirections.add(candidateDirection);
			}
		}
		if (routeDirections.isEmpty()) {
			return false;
		}
		Direction selectedDirection = routeDirections.get(this.level().getRandom().nextInt(routeDirections.size()));
		RoutePlan selectedRoute = this.findNearestJunction(selectedDirection, junctionPos);
		if (selectedRoute == null) {
			return false;
		}
		this.applyRoute(selectedRoute, selectedDirection);
		return true;
	}

	public boolean startRouteTo(BlockPos junctionPos) {
		return this.startRouteTo(junctionPos, List.of());
	}

	public boolean startRouteTo(BlockPos junctionPos, List<BlockPos> callerContacts) {
		return this.startRouteTo(junctionPos, callerContacts, List.of());
	}

	public boolean startRouteTo(BlockPos junctionPos, List<BlockPos> callerContacts, List<BlockPos> callerFlightAnchors) {
		LOGGER.info("Planning a called route from Mover {} to Junction {}.", this.blockPosition(), junctionPos);
		BlockState junctionState = this.level().getBlockState(junctionPos);
		if (!MoverBlock.isValidJunction(junctionState)) {
			LOGGER.warn("Called route from Mover {} failed because {} is not a valid Junction.", this.blockPosition(), junctionPos);
			return false;
		}
		if (!callerContacts.isEmpty()) {
			List<QueuedRoute> contactRoute = this.createCallerContactRoute(junctionPos, callerContacts, callerFlightAnchors);
			if (!contactRoute.isEmpty()) {
				return this.beginCalledRoute(junctionPos, contactRoute, "the caller's confirmed contact chain");
			}
			LOGGER.warn("Caller Junction {} supplied a contact chain that Mover {} could not execute.", junctionPos, this.blockPosition());
			return false;
		}
		RouteTarget route = this.findRouteToJunction(junctionPos, junctionState);
		if (route != null) {
			Direction routeDirection = this.directionTo(route.contactPos());
			if (routeDirection == null) {
				return false;
			}
			this.queuedRoutes.clear();
			this.applyRoute(new RoutePlan(route, List.of()), routeDirection);
			this.autonomous = false;
			this.setDirection(routeDirection);
			this.anchorPos = junctionPos;
			this.anchorRange = JUNCTION_ROUTE_RANGE;
			this.beginRoutePreview();
			LOGGER.info("Called route from Mover {} to Junction {} uses a direct segment ending at {}.", this.blockPosition(), junctionPos, route.contactPos());
			return true;
		}
		List<QueuedRoute> routePath = this.findCalledRoute(junctionPos);
		if (routePath.isEmpty()) {
			LOGGER.warn("Called route from Mover {} could not find a visible-anchor path to Junction {}.", this.blockPosition(), junctionPos);
			return false;
		}
		return this.beginCalledRoute(junctionPos, routePath, "a visible-anchor search");
	}

	private boolean beginCalledRoute(BlockPos junctionPos, List<QueuedRoute> routePath, String source) {
		QueuedRoute firstRoute = routePath.removeFirst();
		this.queuedRoutes.clear();
		this.queuedRoutes.addAll(routePath);
		this.applyQueuedRoute(firstRoute);
		this.autonomous = false;
		this.anchorPos = junctionPos;
		this.anchorRange = JUNCTION_ROUTE_RANGE;
		this.beginRoutePreview();
		LOGGER.info("Called route from Mover {} to Junction {} uses {} with {} segments.", this.blockPosition(), junctionPos, source, routePath.size() + 1);
		return true;
	}

	private void tickMovement(ServerLevel level) {
		if (this.isPreviewingRoute()) {
			if (this.routePreviewTicks > 0) {
				this.routePreviewTicks--;
				return;
			}
			this.clearRoutePreview();
		}
		if (this.target != null) {
			this.ensureRouteFlightAnchors();
			this.moveTowardTarget(level);
			this.updateControlState();
			return;
		}
		this.setSpeed(0.0F);
		this.setDeltaMovement(Vec3.ZERO);
		this.updateAnchor();
		if (this.autonomous) {
			if (!this.beginRoute(this.forwardDirection())) {
				this.autonomous = false;
			}
			return;
		}
		if (this.getControllingPassenger() == null) {
			this.settleAsBlock(level);
			return;
		}
		this.handleRiderInput();
		this.updateRouteSignPreview();
	}

	private void tickLinkedFollower(ServerLevel level) {
		Entity leaderEntity = level.getEntity(this.linkLeaderId);
		if (!(leaderEntity instanceof MoverEntity leader) || leader.isRemoved()) {
			this.settleAsBlock(level);
			return;
		}
		Vec3 destination = leader.position().add(this.linkOffset);
		this.clearMovingPath(level, destination.subtract(this.position()));
		AABB destinationBox = this.getBoundingBox().move(destination.subtract(this.position()));
		if (!level.noCollision(this, destinationBox)) {
			this.detachFromLeader(level, leader);
			return;
		}
		this.setDirection(leader.direction);
		this.setSpeed(leader.getMoverSpeed());
		this.setDeltaMovement(destination.subtract(this.position()));
		this.setPos(destination);
		if (this.getMoverSpeed() > 0.0F) {
			this.damageEntities(level, this.getMoverSpeed());
		}
	}

	private void detachFromLeader(ServerLevel level, MoverEntity leader) {
		leader.removeLinkedFollower(this);
		this.linkLeaderId = null;
		this.linkOffset = Vec3.ZERO;
		this.target = null;
		this.queuedRoutes.clear();
		this.routeFlightAnchors = List.of();
		this.clearRoutePreview();
		this.autonomous = false;
		this.setSpeed(0.0F);
		this.setDeltaMovement(Vec3.ZERO);
		if (this.getControllingPassenger() == null) {
			this.settleAsBlock(level);
		}
	}

	private void handleRiderInput() {
		if (!(this.getControllingPassenger() instanceof ServerPlayer player)) {
			return;
		}
		Input input = player.getLastClientInput();
		boolean forwardPressed = input.forward();
		boolean backwardPressed = input.backward();
		if (forwardPressed && !this.forwardWasPressed && !backwardPressed) {
			Direction routeDirection = this.controlDirection(player);
			this.beginRoute(routeDirection);
		}
		if (backwardPressed && !this.backwardWasPressed && !forwardPressed) {
			Direction routeDirection = this.controlDirection(player).getOpposite();
			this.beginRoute(routeDirection);
		}
		this.forwardWasPressed = forwardPressed;
		this.backwardWasPressed = backwardPressed;
	}

	private void updateControlState() {
		if (!(this.getControllingPassenger() instanceof ServerPlayer player)) {
			this.forwardWasPressed = false;
			this.backwardWasPressed = false;
			return;
		}
		Input input = player.getLastClientInput();
		this.forwardWasPressed = input.forward();
		this.backwardWasPressed = input.backward();
	}

	private Direction controlDirection(Player player) {
		Vec3 look = player.getLookAngle();
		if (Math.abs(look.y) > Math.max(Math.abs(look.x), Math.abs(look.z))) {
			return look.y > 0.0D ? Direction.UP : Direction.DOWN;
		}
		if (Math.abs(look.x) > Math.abs(look.z)) {
			return look.x > 0.0D ? Direction.EAST : Direction.WEST;
		}
		return look.z > 0.0D ? Direction.SOUTH : Direction.NORTH;
	}

	private Direction directionTo(BlockPos target) {
		int xDistance = target.getX() - this.blockPosition().getX();
		int yDistance = target.getY() - this.blockPosition().getY();
		int zDistance = target.getZ() - this.blockPosition().getZ();
		if (xDistance != 0 && yDistance == 0 && zDistance == 0) {
			return xDistance > 0 ? Direction.EAST : Direction.WEST;
		}
		if (xDistance == 0 && yDistance != 0 && zDistance == 0) {
			return yDistance > 0 ? Direction.UP : Direction.DOWN;
		}
		if (xDistance == 0 && yDistance == 0 && zDistance != 0) {
			return zDistance > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		return null;
	}

	private void tickClientMovement() {
		float speed = this.getMoverSpeed();
		if (speed <= 0.0F) {
			this.setDeltaMovement(Vec3.ZERO);
			return;
		}
		Vec3 velocity = new Vec3(
			this.direction.getStepX() * speed,
			this.direction.getStepY() * speed,
			this.direction.getStepZ() * speed
		);
		this.setDeltaMovement(velocity);
		this.setPos(this.position().add(velocity));
	}

	private boolean beginRoute(Direction routeDirection) {
		RoutePlan routePlan = this.findNearestJunction(routeDirection);
		if (routePlan == null) {
			return false;
		}
		this.applyRoute(routePlan, routeDirection);
		return true;
	}

	private void applyRoute(RoutePlan routePlan, Direction routeDirection) {
		this.target = routePlan.target();
		this.routeFlightAnchors = routePlan.flightAnchors();
		this.setDirection(routeDirection);
		RouteTarget currentJunction = this.findAdjacentJunction();
		this.anchorPos = currentJunction == null ? this.blockPosition() : currentJunction.junctionPos();
		this.anchorRange = this.routeRange();
		this.updateRoutePreviewSegments();
	}

	private void applyQueuedRoute(QueuedRoute queuedRoute) {
		RouteTarget queuedTarget = queuedRoute.target();
		this.applyRoute(new RoutePlan(queuedTarget, queuedRoute.flightAnchors()), queuedRoute.direction());
		LOGGER.info("Mover {} begins a cardinal route segment toward {}.", this.blockPosition(), queuedTarget.contactPos());
	}

	private void moveTowardTarget(ServerLevel level) {
		Vec3 targetPosition = this.target.position();
		Vec3 remaining = targetPosition.subtract(this.position());
		double speedLimit = this.isMovingThroughFluid() ? MAX_SPEED * 0.5D : MAX_SPEED;
		double speed = Math.min(speedLimit, this.getMoverSpeed() + ACCELERATION);
		double remainingDistance = remaining.length();
		if (remainingDistance == 0.0D) {
			this.finishRoute(level);
			return;
		}
		double movementDistance = Math.min(remainingDistance, speed);
		Vec3 velocity = remaining.normalize().scale(movementDistance);
		this.clearMovingPath(level, velocity);
		BlockPos nextPos = BlockPos.containing(this.position().add(velocity));
		BlockState nextState = level.getBlockState(nextPos);
		if (!this.canTravelTo(level, nextPos, nextState)) {
			LOGGER.warn("Mover {} stopped before route target {} because {} blocks its next position {}.", this.blockPosition(), this.target.contactPos(), nextState.getBlock(), nextPos);
			this.stopAtCurrentPosition(level, nextState);
			return;
		}
		if (!this.isWithinRouteFlightRoom(nextPos)) {
			LOGGER.warn("Mover {} stopped before route target {} because its next position {} is outside route flight room.", this.blockPosition(), this.target.contactPos(), nextPos);
			this.stopAtCurrentPosition(level, nextState);
			return;
		}
		if (remainingDistance <= speed) {
			this.setPos(targetPosition);
			this.finishRoute(level);
			return;
		}
		this.setSpeed((float) speed);
		this.setDeltaMovement(velocity);
		this.move(MoverType.SELF, velocity);
		if (this.horizontalCollision || this.direction.getAxis().isVertical() && this.verticalCollision) {
			LOGGER.warn("Mover {} collided while travelling toward route target {}.", this.blockPosition(), this.target.contactPos());
			this.stopAtCurrentPosition(level, nextState);
			return;
		}
		this.damageEntities(level, speed);
	}

	private void finishRoute(ServerLevel level) {
		boolean reachedJunction = this.target.junctionPos() != null;
		if (this.target.junctionPos() != null) {
			BlockState junctionState = level.getBlockState(this.target.junctionPos());
			this.setDirection(MoverBlock.getJunctionDirection(junctionState, this.target.junctionPos(), this.target.contactPos()));
			this.anchorPos = this.target.junctionPos();
			this.anchorRange = this.routeRange();
		}
		if (reachedJunction && this.getFirstPassenger() instanceof ServerPlayer player) {
			GranulesAdvancements.award(player, "people_mover");
			if (this.linkedFollowerIds.size() >= 3) {
				GranulesAdvancements.award(player, "with_friends");
			}
		}
		this.target = null;
		this.routeFlightAnchors = List.of();
		if (!this.queuedRoutes.isEmpty()) {
			this.setSpeed(0.0F);
			this.setDeltaMovement(Vec3.ZERO);
			this.applyQueuedRoute(this.queuedRoutes.removeFirst());
			return;
		}
		this.clearRoutePreview();
		this.setSpeed(0.0F);
		this.setDeltaMovement(Vec3.ZERO);
		if (this.autonomous && !this.beginRoute(this.forwardDirection())) {
			this.autonomous = false;
		}
	}

	private RoutePlan findNearestJunction(Direction searchDirection) {
		return this.findNearestJunction(searchDirection, null);
	}

	private RoutePlan findNearestJunction(Direction searchDirection, BlockPos excludedJunctionPos) {
		return this.findNearestJunction(this.blockPosition(), searchDirection, excludedJunctionPos);
	}

	private RoutePlan findNearestJunction(BlockPos origin, Direction searchDirection, BlockPos excludedJunctionPos) {
		for (int distance = 1; distance <= MAX_ROUTE_SEARCH_DISTANCE; distance++) {
			BlockPos contactPos = origin.relative(searchDirection, distance);
			RouteTarget route = this.findJunctionAtContact(contactPos);
			if (route != null && !route.junctionPos().equals(excludedJunctionPos)) {
				return new RoutePlan(route, List.of());
			}
		}
		return null;
	}

	private List<QueuedRoute> findCalledRoute(BlockPos destinationJunctionPos) {
		Deque<AnchorSearchNode> pendingNodes = new ArrayDeque<>();
		Set<BlockPos> visitedAnchors = new HashSet<>();
		List<RouteAnchor> initialAnchors = this.findVisibleAnchors(this.blockPosition(), LODESTONE_ROUTE_RANGE);
		LOGGER.info("Mover {} sees {} initial route anchors while planning for Junction {}.", this.blockPosition(), initialAnchors.size(), destinationJunctionPos);
		for (RouteAnchor anchor : initialAnchors) {
			pendingNodes.addLast(new AnchorSearchNode(anchor, List.of(anchor)));
			visitedAnchors.add(anchor.pos());
		}
		int visitedNodeCount = 0;
		while (!pendingNodes.isEmpty()) {
			AnchorSearchNode node = pendingNodes.removeFirst();
			visitedNodeCount++;
			if (node.anchor().pos().equals(destinationJunctionPos)) {
				List<QueuedRoute> route = this.createAnchorRoute(node.path());
				LOGGER.info("Mover {} reached caller anchor {} after visiting {} anchors and produced {} route segments.", this.blockPosition(), destinationJunctionPos, visitedNodeCount, route.size());
				return route;
			}
			for (RouteAnchor visibleAnchor : this.findVisibleAnchors(node.anchor().pos(), node.anchor().range())) {
				if (!visitedAnchors.add(visibleAnchor.pos())) {
					continue;
				}
				List<RouteAnchor> path = new ArrayList<>(node.path());
				path.add(visibleAnchor);
				pendingNodes.addLast(new AnchorSearchNode(visibleAnchor, path));
			}
		}
		LOGGER.warn("Mover {} visited {} anchors without finding caller Junction {}.", this.blockPosition(), visitedNodeCount, destinationJunctionPos);
		return List.of();
	}

	private List<RouteAnchor> findVisibleAnchors(BlockPos origin, int range) {
		List<RouteAnchor> anchors = new ArrayList<>();
		for (int xOffset = -range; xOffset <= range; xOffset++) {
			for (int yOffset = -range; yOffset <= range; yOffset++) {
				for (int zOffset = -range; zOffset <= range; zOffset++) {
					BlockPos anchorPos = origin.offset(xOffset, yOffset, zOffset);
					BlockState anchorState = this.level().getBlockState(anchorPos);
					int anchorRange = this.flightRange(anchorState);
					if (anchorRange > 0) {
						anchors.add(new RouteAnchor(anchorPos, anchorRange, MoverBlock.isValidJunction(anchorState)));
					}
				}
			}
		}
		return anchors;
	}

	private List<QueuedRoute> createAnchorRoute(List<RouteAnchor> anchors) {
		if (anchors.isEmpty()) {
			return List.of();
		}
		RouteAnchor destinationAnchor = anchors.getLast();
		if (!destinationAnchor.junction()) {
			return List.of();
		}
		List<FlightAnchor> flightAnchors = this.toFlightAnchors(anchors);
		RouteTarget target = this.findCardinalRouteToJunction(destinationAnchor.pos());
		if (target == null) {
			LOGGER.warn("Mover {} could not form a cardinal route to any contact of caller Junction {}.", this.blockPosition(), destinationAnchor.pos());
			return List.of();
		}
		List<BlockPos> route = this.findCardinalContactRoute(this.blockPosition(), target.contactPos());
		if (route.isEmpty()) {
			LOGGER.warn("Mover {} could not form a cardinal contact path to {} after selecting caller Junction {}.", this.blockPosition(), target.contactPos(), destinationAnchor.pos());
			return List.of();
		}
		List<QueuedRoute> segments = this.compressCardinalRoute(route, target, flightAnchors);
		LOGGER.info("Mover {} formed a cardinal route through {} contacts with {} segments to caller Junction {}.", this.blockPosition(), route.size() - 1, segments.size(), destinationAnchor.pos());
		return segments;
	}

	private List<FlightAnchor> toFlightAnchors(List<RouteAnchor> anchors) {
		List<FlightAnchor> flightAnchors = new ArrayList<>();
		for (RouteAnchor anchor : anchors) {
			flightAnchors.add(new FlightAnchor(anchor.pos(), anchor.range()));
		}
		return flightAnchors;
	}

	private List<QueuedRoute> createCallerContactRoute(BlockPos destinationJunctionPos, List<BlockPos> reverseContacts, List<BlockPos> callerFlightAnchors) {
		List<BlockPos> route = new ArrayList<>();
		route.add(this.blockPosition());
		for (int index = reverseContacts.size() - 1; index >= 0; index--) {
			BlockPos contactPos = reverseContacts.get(index);
			BlockPos previousPos = route.getLast();
			if (contactPos.equals(previousPos)) {
				continue;
			}
			if (this.directionToFrom(previousPos, contactPos) == null) {
				return List.of();
			}
			route.add(contactPos);
		}
		if (route.size() < 2) {
			return List.of();
		}
		BlockPos destinationContactPos = route.getLast();
		BlockState destinationState = this.level().getBlockState(destinationJunctionPos);
		if (!MoverBlock.isValidJunctionContact(destinationState, destinationJunctionPos, destinationContactPos)) {
			return List.of();
		}
		RouteTarget destination = new RouteTarget(destinationJunctionPos, destinationContactPos);
		List<FlightAnchor> flightAnchors = this.findContactFlightAnchors(route);
		this.addCallerFlightAnchors(flightAnchors, callerFlightAnchors);
		List<QueuedRoute> segments = this.compressCardinalRoute(route, destination, flightAnchors);
		LOGGER.info("Mover {} received a caller contact chain of {} contacts and produced {} cardinal segments.", this.blockPosition(), reverseContacts.size(), segments.size());
		return segments;
	}

	private void addCallerFlightAnchors(List<FlightAnchor> flightAnchors, List<BlockPos> callerFlightAnchors) {
		Set<BlockPos> anchorPositions = new HashSet<>();
		for (FlightAnchor flightAnchor : flightAnchors) {
			anchorPositions.add(flightAnchor.pos());
		}
		for (BlockPos anchorPos : callerFlightAnchors) {
			if (!anchorPositions.add(anchorPos)) {
				continue;
			}
			int range = this.flightRange(this.level().getBlockState(anchorPos));
			if (range > 0) {
				flightAnchors.add(new FlightAnchor(anchorPos, range));
			}
		}
	}

	private Direction directionToFrom(BlockPos origin, BlockPos target) {
		int xDistance = target.getX() - origin.getX();
		int yDistance = target.getY() - origin.getY();
		int zDistance = target.getZ() - origin.getZ();
		if (xDistance != 0 && yDistance == 0 && zDistance == 0) {
			return xDistance > 0 ? Direction.EAST : Direction.WEST;
		}
		if (xDistance == 0 && yDistance != 0 && zDistance == 0) {
			return yDistance > 0 ? Direction.UP : Direction.DOWN;
		}
		if (xDistance == 0 && yDistance == 0 && zDistance != 0) {
			return zDistance > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		return null;
	}

	private List<FlightAnchor> findContactFlightAnchors(List<BlockPos> contacts) {
		List<FlightAnchor> flightAnchors = new ArrayList<>();
		Set<BlockPos> anchorPositions = new HashSet<>();
		for (BlockPos contactPos : contacts) {
			RouteTarget contact = this.findJunctionAtContact(contactPos);
			if (contact != null && anchorPositions.add(contact.junctionPos())) {
				int range = this.flightRange(this.level().getBlockState(contact.junctionPos()));
				if (range > 0) {
					flightAnchors.add(new FlightAnchor(contact.junctionPos(), range));
				}
			}
			for (Direction direction : Direction.values()) {
				BlockPos lodestonePos = contactPos.relative(direction);
				if (this.level().getBlockState(lodestonePos).is(Blocks.LODESTONE) && anchorPositions.add(lodestonePos)) {
					flightAnchors.add(new FlightAnchor(lodestonePos, LODESTONE_ROUTE_RANGE));
				}
			}
		}
		return flightAnchors;
	}

	private RouteTarget findCardinalRouteToJunction(BlockPos junctionPos) {
		BlockState junctionState = this.level().getBlockState(junctionPos);
		RouteTarget shortestRoute = null;
		int shortestDistance = Integer.MAX_VALUE;
		for (Direction contactDirection : Direction.values()) {
			BlockPos contactPos = junctionPos.relative(contactDirection);
			if (!MoverBlock.isValidJunctionContact(junctionState, junctionPos, contactPos)) {
				continue;
			}
			List<BlockPos> route = this.findCardinalContactRoute(this.blockPosition(), contactPos);
			int routeDistance = cardinalRouteDistance(route);
			if (route.isEmpty() || routeDistance >= shortestDistance) {
				continue;
			}
			shortestRoute = new RouteTarget(junctionPos, contactPos);
			shortestDistance = routeDistance;
		}
		return shortestRoute;
	}

	private List<BlockPos> findCardinalContactRoute(BlockPos origin, BlockPos destination) {
		PriorityQueue<GridSearchNode> pendingNodes = new PriorityQueue<>(Comparator.comparingInt(GridSearchNode::distance));
		Map<BlockPos, BlockPos> previousPositions = new HashMap<>();
		Map<BlockPos, Integer> bestDistances = new HashMap<>();
		pendingNodes.add(new GridSearchNode(origin, 0));
		previousPositions.put(origin, origin);
		bestDistances.put(origin, 0);
		while (!pendingNodes.isEmpty() && previousPositions.size() <= MAX_GRID_ROUTE_NODES) {
			GridSearchNode node = pendingNodes.remove();
			if (node.distance() != bestDistances.getOrDefault(node.pos(), Integer.MAX_VALUE)) {
				continue;
			}
			if (node.pos().equals(destination)) {
				return reconstructCardinalRoute(previousPositions, destination);
			}
			for (Direction direction : Direction.values()) {
				for (ReachableContact contact : this.findReachableContacts(node.pos(), destination, direction)) {
					int distance = node.distance() + contact.distance();
					if (distance > MAX_ROUTE_SEARCH_DISTANCE || distance >= bestDistances.getOrDefault(contact.pos(), Integer.MAX_VALUE)) {
						continue;
					}
					previousPositions.put(contact.pos(), node.pos());
					bestDistances.put(contact.pos(), distance);
					pendingNodes.add(new GridSearchNode(contact.pos(), distance));
				}
			}
		}
		return List.of();
	}

	private List<ReachableContact> findReachableContacts(BlockPos origin, BlockPos destination, Direction direction) {
		List<ReachableContact> contacts = new ArrayList<>();
		for (int distance = 1; distance <= MAX_ROUTE_SEARCH_DISTANCE; distance++) {
			BlockPos candidatePos = origin.relative(direction, distance);
			if (!this.canPlanRouteThrough(candidatePos)) {
				return contacts;
			}
			if (candidatePos.equals(destination)) {
				contacts.add(new ReachableContact(candidatePos, distance));
				return contacts;
			}
			if (this.findJunctionAtContact(candidatePos) != null) {
				contacts.add(new ReachableContact(candidatePos, distance));
			}
		}
		return contacts;
	}

	private static List<BlockPos> reconstructCardinalRoute(Map<BlockPos, BlockPos> previousPositions, BlockPos destination) {
		List<BlockPos> route = new ArrayList<>();
		BlockPos currentPos = destination;
		while (true) {
			route.add(currentPos);
			BlockPos previousPos = previousPositions.get(currentPos);
			if (previousPos == null || previousPos.equals(currentPos)) {
				break;
			}
			currentPos = previousPos;
		}
		java.util.Collections.reverse(route);
		return route;
	}

	private boolean canPlanRouteThrough(BlockPos pos) {
		if (this.isOccupiedByAnotherMover(pos)) {
			return false;
		}
		BlockState state = this.level().getBlockState(pos);
		if (state.isAir() || !state.getFluidState().isEmpty()) {
			return true;
		}
		return state.getDestroySpeed(this.level(), pos) <= 0.0F;
	}

	private static int cardinalRouteDistance(List<BlockPos> route) {
		int distance = 0;
		for (int index = 1; index < route.size(); index++) {
			distance += routeDistanceBetween(route.get(index - 1), route.get(index));
		}
		return distance;
	}

	private List<QueuedRoute> compressCardinalRoute(List<BlockPos> route, RouteTarget destination, List<FlightAnchor> flightAnchors) {
		if (route.size() < 2) {
			return List.of();
		}
		List<QueuedRoute> segments = new ArrayList<>();
		Direction segmentDirection = directionFromTo(route.getFirst(), route.get(1));
		for (int index = 2; index < route.size(); index++) {
			Direction nextDirection = directionFromTo(route.get(index - 1), route.get(index));
			if (nextDirection == segmentDirection) {
				continue;
			}
			BlockPos turnPos = route.get(index - 1);
			RouteTarget turnTarget = this.findJunctionAtContact(turnPos);
			if (turnTarget == null) {
				turnTarget = new RouteTarget(null, turnPos);
			}
			segments.add(new QueuedRoute(turnTarget, segmentDirection, flightAnchors));
			segmentDirection = nextDirection;
		}
		segments.add(new QueuedRoute(destination, segmentDirection, flightAnchors));
		return segments;
	}

	private static Direction directionFromTo(BlockPos origin, BlockPos target) {
		int xDistance = target.getX() - origin.getX();
		int yDistance = target.getY() - origin.getY();
		int zDistance = target.getZ() - origin.getZ();
		if (Math.abs(yDistance) >= Math.max(Math.abs(xDistance), Math.abs(zDistance))) {
			return yDistance >= 0 ? Direction.UP : Direction.DOWN;
		}
		if (Math.abs(xDistance) >= Math.abs(zDistance)) {
			return xDistance >= 0 ? Direction.EAST : Direction.WEST;
		}
		return zDistance >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private void updateRouteSignPreview() {
		if (!(this.getControllingPassenger() instanceof ServerPlayer player)) {
			return;
		}
		Direction lookDirection = this.controlDirection(player);
		if (this.previewDirection != lookDirection
			|| !this.previewOrigin.equals(this.blockPosition())
			|| !this.previewAnchorPos.equals(this.anchorPos)
			|| this.previewAnchorRange != this.anchorRange
			|| this.previewFlightAnchors.isEmpty()) {
			this.previewDirection = lookDirection;
			this.previewOrigin = this.blockPosition();
			this.previewAnchorPos = this.anchorPos;
			this.previewAnchorRange = this.anchorRange;
			this.previewFlightAnchors = this.findKnownFlightAnchors();
			this.routeSignPreview = this.findRouteSign(player, lookDirection, this.previewFlightAnchors);
		}
		if (this.routeSignPreview != null) {
			player.sendOverlayMessage(this.routeSignPreview);
		}
	}

	private Component findRouteSign(ServerPlayer player, Direction routeDirection, List<FlightAnchor> flightAnchors) {
		Set<BlockPos> visitedJunctions = new HashSet<>();
		int stopsAway = 0;
		RouteTarget attachedJunction = this.findAdjacentJunction();
		if (attachedJunction != null) {
			visitedJunctions.add(attachedJunction.junctionPos());
		}
		for (RouteSignCandidate candidate : this.findForwardJunctionSignCandidates(routeDirection, flightAnchors)) {
			RouteTarget route = candidate.route();
			if (!visitedJunctions.add(route.junctionPos())) {
				continue;
			}
			stopsAway++;
			SignBlockEntity sign = this.findNearestJunctionSign(route.junctionPos());
			if (sign != null) {
				return formatSignMessage(sign, player, stopsAway);
			}
		}
		return null;
	}

	private List<RouteSignCandidate> findForwardJunctionSignCandidates(Direction routeDirection, List<FlightAnchor> flightAnchors) {
		Map<BlockPos, RouteSignCandidate> candidatesByJunction = new HashMap<>();
		int maximumDistance = this.maximumSignPreviewDistance(routeDirection, flightAnchors);
		Direction firstPerpendicular = this.firstPerpendicular(routeDirection);
		Direction secondPerpendicular = this.secondPerpendicular(routeDirection);
		for (int forwardDistance = 1; forwardDistance <= maximumDistance; forwardDistance++) {
			int lateralTolerance = Math.min(4, Math.max(1, forwardDistance / 2));
			for (int firstOffset = -lateralTolerance; firstOffset <= lateralTolerance; firstOffset++) {
				for (int secondOffset = -lateralTolerance; secondOffset <= lateralTolerance; secondOffset++) {
					BlockPos contactPos = this.blockPosition()
						.relative(routeDirection, forwardDistance)
						.relative(firstPerpendicular, firstOffset)
						.relative(secondPerpendicular, secondOffset);
					if (!this.isWithinFlightRoom(contactPos, flightAnchors)) {
						continue;
					}
					RouteTarget route = this.findJunctionAtPreviewContact(contactPos);
					if (route == null) {
						continue;
					}
					int lateralDistance = Math.abs(firstOffset) + Math.abs(secondOffset);
					RouteSignCandidate candidate = new RouteSignCandidate(route, forwardDistance, lateralDistance);
					RouteSignCandidate currentCandidate = candidatesByJunction.get(route.junctionPos());
					if (currentCandidate == null || candidate.isCloserThan(currentCandidate)) {
						candidatesByJunction.put(route.junctionPos(), candidate);
					}
				}
			}
		}
		List<RouteSignCandidate> candidates = new ArrayList<>(candidatesByJunction.values());
		candidates.sort(Comparator
			.comparingInt(RouteSignCandidate::forwardDistance)
			.thenComparingInt(RouteSignCandidate::lateralDistance));
		return candidates;
	}

	private int maximumSignPreviewDistance(Direction routeDirection, List<FlightAnchor> flightAnchors) {
		int maximumDistance = 0;
		for (FlightAnchor flightAnchor : flightAnchors) {
			BlockPos anchorPos = flightAnchor.pos();
			int forwardOffset = (anchorPos.getX() - this.blockPosition().getX()) * routeDirection.getStepX()
				+ (anchorPos.getY() - this.blockPosition().getY()) * routeDirection.getStepY()
				+ (anchorPos.getZ() - this.blockPosition().getZ()) * routeDirection.getStepZ();
			maximumDistance = Math.max(maximumDistance, forwardOffset + flightAnchor.range());
		}
		return Math.min(MAX_ROUTE_SEARCH_DISTANCE, Math.max(0, maximumDistance));
	}

	private SignBlockEntity findNearestJunctionSign(BlockPos junctionPos) {
		SignBlockEntity nearestSign = null;
		int nearestDistanceSquared = Integer.MAX_VALUE;
		int signRadius = MoverConfig.get().bellSummonRadius();
		for (int xOffset = -signRadius; xOffset <= signRadius; xOffset++) {
			for (int yOffset = -signRadius; yOffset <= signRadius; yOffset++) {
				for (int zOffset = -signRadius; zOffset <= signRadius; zOffset++) {
					int distanceSquared = xOffset * xOffset + yOffset * yOffset + zOffset * zOffset;
					if (distanceSquared > signRadius * signRadius || distanceSquared >= nearestDistanceSquared) {
						continue;
					}
					BlockPos signPos = junctionPos.offset(xOffset, yOffset, zOffset);
					if (this.level().getBlockEntity(signPos) instanceof SignBlockEntity sign) {
						nearestSign = sign;
						nearestDistanceSquared = distanceSquared;
					}
				}
			}
		}
		return nearestSign;
	}

	private Component formatSignMessage(SignBlockEntity sign, Player player, int stopsAway) {
		net.minecraft.network.chat.MutableComponent message = Component.empty();
		Component label = this.firstWrittenSignLine(sign, player);
		if (label != null) {
			message.append(label);
			message.append(Component.literal("  —  "));
		}
		String stopLabel = stopsAway == 1 ? "stop" : "stops";
		message.append(Component.literal("(" + stopsAway + " " + stopLabel + " away)"));
		return message;
	}

	private Component firstWrittenSignLine(SignBlockEntity sign, Player player) {
		boolean facingFront = sign.isFacingFrontText(player);
		Component label = firstNonemptySignLine(sign.getText(facingFront).getMessages(false));
		if (label != null) {
			return label;
		}
		return firstNonemptySignLine(sign.getText(!facingFront).getMessages(false));
	}

	private static Component firstNonemptySignLine(Component[] lines) {
		for (Component line : lines) {
			if (!line.getString().isBlank()) {
				return line;
			}
		}
		return null;
	}

	private RouteTarget findRouteToJunction(BlockPos junctionPos, BlockState junctionState) {
		for (Direction contactDirection : Direction.values()) {
			BlockPos contactPos = junctionPos.relative(contactDirection);
			if (contactPos.equals(this.blockPosition())) {
				continue;
			}
			if (this.isOccupiedByAnotherMover(contactPos)) {
				continue;
			}
			if (!MoverBlock.isValidJunctionContact(junctionState, junctionPos, contactPos)) {
				continue;
			}
			if (this.directionTo(contactPos) != null) {
				return new RouteTarget(junctionPos, contactPos);
			}
		}
		return null;
	}

	private RouteTarget findJunctionAtContact(BlockPos contactPos) {
		if (this.isOccupiedByAnotherMover(contactPos)) {
			return null;
		}
		for (Direction contactDirection : Direction.values()) {
			BlockPos junctionPos = contactPos.relative(contactDirection);
			BlockState junctionState = this.level().getBlockState(junctionPos);
			if (MoverBlock.isValidJunctionContact(junctionState, junctionPos, contactPos)) {
				return new RouteTarget(junctionPos, contactPos);
			}
		}
		return null;
	}

	private RouteTarget findJunctionAtPreviewContact(BlockPos contactPos) {
		for (Direction contactDirection : Direction.values()) {
			BlockPos junctionPos = contactPos.relative(contactDirection);
			BlockState junctionState = this.level().getBlockState(junctionPos);
			if (MoverBlock.isValidJunctionContact(junctionState, junctionPos, contactPos)) {
				return new RouteTarget(junctionPos, contactPos);
			}
		}
		return null;
	}

	private boolean isOccupiedByAnotherMover(BlockPos pos) {
		if (!pos.equals(this.blockPosition()) && this.level().getBlockState(pos).is(GranulesMod.MOVER)) {
			return true;
		}
		return !this.level().getEntitiesOfClass(MoverEntity.class, new AABB(pos), mover -> mover != this).isEmpty();
	}


	private List<FlightAnchor> findFlightAnchors(Direction routeDirection, int routeDistance) {
		List<FlightAnchor> knownAnchors = this.findKnownFlightAnchors();
		if (this.isRouteCoveredByAnchors(routeDirection, routeDistance, knownAnchors)) {
			return knownAnchors;
		}
		return this.findFlightAnchorsBySearch(this.blockPosition(), routeDirection, routeDistance);
	}

	private List<FlightAnchor> findKnownFlightAnchors() {
		List<FlightAnchor> knownAnchors = new ArrayList<>();
		if (this.anchorRange > 0) {
			knownAnchors.add(new FlightAnchor(this.anchorPos, this.anchorRange));
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			MoverEntity groupLeader = this.getGroupLeader();
			if (groupLeader == this) {
				for (MoverEntity mover : this.getLinkedMovers(serverLevel)) {
					this.addAttachedFlightAnchors(knownAnchors, mover.blockPosition());
				}
			}
		}
		if (this.target != null) {
			BlockPos targetJunctionPos = this.target.junctionPos();
			if (targetJunctionPos != null) {
				int targetRange = this.flightRange(this.level().getBlockState(targetJunctionPos));
				if (targetRange > 0 && !targetJunctionPos.equals(this.anchorPos)) {
					knownAnchors.add(new FlightAnchor(targetJunctionPos, targetRange));
				}
			}
		}
		return knownAnchors;
	}

	private void addAttachedFlightAnchors(List<FlightAnchor> flightAnchors, BlockPos moverPos) {
		for (Direction direction : Direction.values()) {
			BlockPos anchorPos = moverPos.relative(direction);
			BlockState anchorState = this.level().getBlockState(anchorPos);
			if (!MoverBlock.isValidJunctionContact(anchorState, anchorPos, moverPos) && !anchorState.is(Blocks.LODESTONE)) {
				continue;
			}
			int range = this.flightRange(anchorState);
			if (range > 0 && flightAnchors.stream().noneMatch(anchor -> anchor.pos().equals(anchorPos))) {
				flightAnchors.add(new FlightAnchor(anchorPos, range));
			}
		}
	}

	private boolean isRouteCoveredByAnchors(Direction routeDirection, int routeDistance, List<FlightAnchor> flightAnchors) {
		if (flightAnchors.isEmpty()) {
			return false;
		}
		for (int distance = 0; distance <= routeDistance; distance++) {
			if (!this.isWithinFlightRoom(this.blockPosition().relative(routeDirection, distance), flightAnchors)) {
				return false;
			}
		}
		return true;
	}

	private List<FlightAnchor> findFlightAnchorsBySearch(BlockPos origin, Direction routeDirection, int routeDistance) {
		int maximumFlightRange = LODESTONE_ROUTE_RANGE;
		Direction firstPerpendicular = firstPerpendicular(routeDirection);
		Direction secondPerpendicular = secondPerpendicular(routeDirection);
		List<FlightAnchor> flightAnchors = new ArrayList<>();
		for (int longitudinalOffset = -maximumFlightRange; longitudinalOffset <= routeDistance + maximumFlightRange; longitudinalOffset++) {
			for (int firstOffset = -maximumFlightRange; firstOffset <= maximumFlightRange; firstOffset++) {
				for (int secondOffset = -maximumFlightRange; secondOffset <= maximumFlightRange; secondOffset++) {
					BlockPos candidate = origin
						.relative(routeDirection, longitudinalOffset)
						.relative(firstPerpendicular, firstOffset)
						.relative(secondPerpendicular, secondOffset);
					BlockState state = this.level().getBlockState(candidate);
					int flightRange = flightRange(state);
					if (flightRange > 0) {
						flightAnchors.add(new FlightAnchor(candidate, flightRange));
					}
				}
			}
		}
		return flightAnchors;
	}

	private Direction firstPerpendicular(Direction routeDirection) {
		if (routeDirection.getAxis() == Direction.Axis.Y) {
			return Direction.EAST;
		}
		return Direction.UP;
	}

	private Direction secondPerpendicular(Direction routeDirection) {
		if (routeDirection.getAxis() == Direction.Axis.X) {
			return Direction.SOUTH;
		}
		if (routeDirection.getAxis() == Direction.Axis.Z) {
			return Direction.EAST;
		}
		return Direction.SOUTH;
	}

	private int flightRange(BlockState state) {
		if (state.is(Blocks.LODESTONE)) {
			return LODESTONE_ROUTE_RANGE;
		}
		if (MoverBlock.isValidJunction(state)) {
			return JUNCTION_ROUTE_RANGE;
		}
		return 0;
	}

	private boolean isWithinRouteFlightRoom(BlockPos pos) {
		return this.isWithinFlightRoom(pos, this.routeFlightAnchors);
	}

	private boolean isWithinFlightRoom(BlockPos pos, List<FlightAnchor> flightAnchors) {
		for (FlightAnchor flightAnchor : flightAnchors) {
			if (flightAnchor.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	private int routeDistanceTo(BlockPos targetPos) {
		return routeDistanceBetween(this.blockPosition(), targetPos);
	}

	private static int routeDistanceBetween(BlockPos firstPos, BlockPos secondPos) {
		return Math.abs(secondPos.getX() - firstPos.getX())
			+ Math.abs(secondPos.getY() - firstPos.getY())
			+ Math.abs(secondPos.getZ() - firstPos.getZ());
	}

	private void ensureRouteFlightAnchors() {
		if (this.target == null || !this.routeFlightAnchors.isEmpty()) {
			return;
		}
		Direction routeDirection = this.directionTo(this.target.contactPos());
		if (routeDirection == null) {
			return;
		}
		this.routeFlightAnchors = this.findFlightAnchors(routeDirection, this.routeDistanceTo(this.target.contactPos()));
	}

	private RouteTarget findAdjacentJunction() {
		for (Direction contactDirection : Direction.values()) {
			BlockPos junctionPos = this.blockPosition().relative(contactDirection);
			BlockState state = this.level().getBlockState(junctionPos);
			if (MoverBlock.isValidJunctionContact(state, junctionPos, this.blockPosition())) {
				return new RouteTarget(junctionPos, this.blockPosition());
			}
		}
		return null;
	}

	private Direction forwardDirection() {
		RouteTarget junction = this.findAdjacentJunction();
		if (junction == null) {
			return this.direction;
		}
		return MoverBlock.getJunctionDirection(
			this.level().getBlockState(junction.junctionPos()),
			junction.junctionPos(),
			this.blockPosition()
		);
	}

	private int routeRange() {
		for (Direction direction : Direction.values()) {
			if (this.level().getBlockState(this.blockPosition().relative(direction)).is(Blocks.LODESTONE)) {
				return LODESTONE_ROUTE_RANGE;
			}
		}
		return JUNCTION_ROUTE_RANGE;
	}

	private boolean canTravelTo(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || !state.getFluidState().isEmpty()) {
			return true;
		}
		if (state.is(GranulesMod.MOVER)) {
			return this.pickUpStationaryMover(level, pos);
		}
		if (this.isCuttableBlock(level, pos, state)) {
			level.destroyBlock(pos, true, this);
			return true;
		}
		return false;
	}

	private void clearMovingPath(ServerLevel level, Vec3 velocity) {
		AABB currentBox = this.getBoundingBox();
		AABB destinationBox = currentBox.move(velocity);
		int minimumX = Mth.floor(Math.min(currentBox.minX, destinationBox.minX) + 0.001D);
		int minimumY = Mth.floor(Math.min(currentBox.minY, destinationBox.minY) + 0.001D);
		int minimumZ = Mth.floor(Math.min(currentBox.minZ, destinationBox.minZ) + 0.001D);
		int maximumX = Mth.floor(Math.max(currentBox.maxX, destinationBox.maxX) - 0.001D);
		int maximumY = Mth.floor(Math.max(currentBox.maxY, destinationBox.maxY) - 0.001D);
		int maximumZ = Mth.floor(Math.max(currentBox.maxZ, destinationBox.maxZ) - 0.001D);
		for (BlockPos blockPos : BlockPos.betweenClosed(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ)) {
			BlockState state = level.getBlockState(blockPos);
			if (state.is(GranulesMod.MOVER)) {
				this.pickUpStationaryMover(level, blockPos);
				continue;
			}
			if (this.isCuttableBlock(level, blockPos, state)) {
				level.destroyBlock(blockPos, true, this);
			}
		}
	}

	private boolean isCuttableBlock(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || !state.getFluidState().isEmpty() || MoverBlock.isAttachmentBlock(state)) {
			return false;
		}
		return state.getDestroySpeed(level, pos) == 0.0F;
	}

	private boolean pickUpStationaryMover(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(GranulesMod.MOVER) || !level.removeBlock(pos, false)) {
			return false;
		}
		MoverEntity leader = this.getGroupLeader();
		if (leader == null) {
			level.setBlock(pos, GranulesMod.MOVER.defaultBlockState(), Block.UPDATE_ALL);
			return false;
		}
		MoverEntity follower = new MoverEntity(level, pos, this.direction);
		follower.follow(leader);
		level.addFreshEntity(follower);
		leader.addLinkedFollower(follower);
		return true;
	}

	private void updateAnchor() {
		RouteTarget junction = this.findAdjacentJunction();
		if (junction != null) {
			this.anchorPos = junction.junctionPos();
			this.anchorRange = this.routeRange();
			return;
		}
		for (Direction contactDirection : Direction.values()) {
			BlockPos candidate = this.blockPosition().relative(contactDirection);
			if (this.level().getBlockState(candidate).is(Blocks.LODESTONE)) {
				this.anchorPos = candidate;
				this.anchorRange = LODESTONE_ROUTE_RANGE;
				return;
			}
		}
	}

	private boolean isWithinAnchorRange(BlockPos pos) {
		if (this.anchorRange == 0) {
			return MoverBlock.hasRestingSupport(this.level(), pos);
		}
		return Math.abs(pos.getX() - this.anchorPos.getX()) <= this.anchorRange
			&& Math.abs(pos.getY() - this.anchorPos.getY()) <= this.anchorRange
			&& Math.abs(pos.getZ() - this.anchorPos.getZ()) <= this.anchorRange;
	}

	private void damageEntities(ServerLevel level, double speed) {
		AABB impactBox = this.getBoundingBox().inflate(0.1D);
		for (Entity entity : level.getEntities(this, impactBox, entity -> entity.isAlive() && !entity.isSpectator() && !this.hasPassenger(entity))) {
			if (entity instanceof MoverEntity mover) {
				if (this.sharesMoverLink(mover)) {
					continue;
				}
				this.resolveMoverCollision(level, mover);
				return;
			}
			if (entity instanceof Projectile) {
				continue;
			}
			if (entity instanceof Player player
				&& this.direction == Direction.DOWN
				&& HardHatProtection.protects(player, this.getBoundingBox())) {
				HardHatProtection.absorbImpact(level, player);
				Block.popResource(level, this.blockPosition(), new ItemStack(GranulesMod.MOVER_ITEM));
				this.discard();
				return;
			}
			entity.hurtServer(level, level.damageSources().anvil(this), (float) Math.max(1.0D, speed * 8.0D));
			entity.push(
				this.direction.getStepX() * speed,
				this.direction.getStepY() * speed,
				this.direction.getStepZ() * speed
			);
			this.setSpeed((float) Math.max(0.05D, speed * 0.5D));
		}
	}

	private void resolveMoverCollision(ServerLevel level, MoverEntity collidedMover) {
		MoverEntity thisLeader = this.getGroupLeader();
		MoverEntity collidedLeader = collidedMover.getGroupLeader();
		if (thisLeader == null || collidedLeader == null || thisLeader == collidedLeader) {
			return;
		}
		int thisLinkCount = thisLeader.getLinkedMoverCount();
		int collidedLinkCount = collidedLeader.getLinkedMoverCount();
		if (thisLinkCount == collidedLinkCount) {
			thisLeader.reverse();
			collidedLeader.reverse();
			return;
		}
		MoverEntity largerLink = thisLinkCount > collidedLinkCount ? thisLeader : collidedLeader;
		MoverEntity smallerLink = largerLink == thisLeader ? collidedLeader : thisLeader;
		MoverEntity assimilator = largerLink.getControllingPassenger() == null ? smallerLink : largerLink;
		MoverEntity absorbed = assimilator == largerLink ? smallerLink : largerLink;
		assimilator.assimilate(level, absorbed);
	}

	private void assimilate(ServerLevel level, MoverEntity absorbedLeader) {
		for (MoverEntity absorbedMover : absorbedLeader.getLinkedMovers(level)) {
			if (absorbedMover == this) {
				continue;
			}
			absorbedMover.follow(this);
			this.addLinkedFollower(absorbedMover);
		}
		absorbedLeader.linkedFollowerIds.clear();
		absorbedLeader.target = null;
		absorbedLeader.queuedRoutes.clear();
		absorbedLeader.routeFlightAnchors = List.of();
		absorbedLeader.clearRoutePreview();
		absorbedLeader.autonomous = false;
		absorbedLeader.setSpeed(0.0F);
		absorbedLeader.setDeltaMovement(Vec3.ZERO);
	}

	private void reverse() {
		this.setDirection(this.direction.getOpposite());
		this.target = null;
		this.queuedRoutes.clear();
		this.routeFlightAnchors = List.of();
		this.clearRoutePreview();
		this.autonomous = false;
		this.setSpeed(0.0F);
		this.setDeltaMovement(Vec3.ZERO);
	}

	private void stopAtCurrentPosition(ServerLevel level, BlockState wallState) {
		if (!MoverBlock.isValidJunction(wallState) && !FallingBlock.isFree(wallState)) {
			this.spawnWallImpactParticles(level, wallState);
		}
		this.settleAsBlock(level);
	}

	private void settleAsBlock(ServerLevel level) {
		BlockPos restingPos = this.blockPosition();
		if (level.getBlockState(restingPos).canBeReplaced()) {
			level.setBlock(restingPos, GranulesMod.MOVER.defaultBlockState(), Block.UPDATE_ALL);
		}
		this.discard();
	}

	private void spawnWallImpactParticles(ServerLevel level, BlockState wallState) {
		double x = this.getX() + this.direction.getStepX() * 0.5D;
		double y = this.getY() + 0.5D + this.direction.getStepY() * 0.5D;
		double z = this.getZ() + this.direction.getStepZ() * 0.5D;
		int particles = Math.max(4, Mth.ceil(this.getMoverSpeed() * 12.0F));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, wallState), x, y, z, particles, 0.22D, 0.22D, 0.22D, this.getMoverSpeed() * 0.08D);
	}

	private void setDirection(Direction direction) {
		this.direction = direction;
		this.entityData.set(DIRECTION, (byte) direction.ordinal());
	}

	private void setSpeed(float speed) {
		this.entityData.set(SPEED, speed);
	}

	private void beginRoutePreview() {
		this.routePreviewTicks = ROUTE_PREVIEW_DURATION;
		this.entityData.set(ROUTE_PREVIEWING, true);
	}

	private void updateRoutePreviewSegments() {
		if (this.target == null) {
			this.entityData.set(ROUTE_PREVIEW_SEGMENTS, "");
			return;
		}
		StringBuilder segments = new StringBuilder();
		this.appendRoutePreviewSegment(segments, this.target);
		for (QueuedRoute queuedRoute : this.queuedRoutes) {
			this.appendRoutePreviewSegment(segments, queuedRoute.target());
		}
		this.entityData.set(ROUTE_PREVIEW_SEGMENTS, segments.toString());
	}

	private void appendRoutePreviewSegment(StringBuilder segments, RouteTarget routeTarget) {
		if (!segments.isEmpty()) {
			segments.append(';');
		}
		BlockPos contactPos = routeTarget.contactPos();
		segments.append(contactPos.getX());
		segments.append(',');
		segments.append(contactPos.getY());
		segments.append(',');
		segments.append(contactPos.getZ());
	}

	private void clearRoutePreview() {
		this.routePreviewTicks = 0;
		this.entityData.set(ROUTE_PREVIEW_SEGMENTS, "");
		this.entityData.set(ROUTE_PREVIEWING, false);
	}

	private MoverEntity getGroupLeader() {
		if (!this.isLinkedFollower()) {
			return this;
		}
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		Entity leaderEntity = serverLevel.getEntity(this.linkLeaderId);
		if (leaderEntity instanceof MoverEntity leader && !leader.isRemoved()) {
			return leader;
		}
		return null;
	}

	private Player findGroupCaptain() {
		Player captain = playerPassenger(this);
		long captainOrder = captain == null ? Long.MAX_VALUE : this.boardingOrder;
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return captain;
		}
		for (UUID followerId : this.linkedFollowerIds) {
			Entity followerEntity = serverLevel.getEntity(followerId);
			if (!(followerEntity instanceof MoverEntity follower) || follower.isRemoved()) {
				continue;
			}
			Player followerPassenger = playerPassenger(follower);
			if (followerPassenger != null && follower.boardingOrder < captainOrder) {
				captain = followerPassenger;
				captainOrder = follower.boardingOrder;
			}
		}
		return captain;
	}

	private static Player playerPassenger(MoverEntity mover) {
		Entity passenger = mover.getFirstPassenger();
		if (passenger instanceof Player player) {
			return player;
		}
		return null;
	}

	private String linkedFollowersAsString() {
		StringBuilder followers = new StringBuilder();
		for (UUID followerId : this.linkedFollowerIds) {
			if (!followers.isEmpty()) {
				followers.append(',');
			}
			followers.append(followerId);
		}
		return followers.toString();
	}

	private void readLinkedFollowers(String storedFollowers) {
		if (storedFollowers.isBlank()) {
			return;
		}
		for (String followerId : storedFollowers.split(",")) {
			try {
				this.linkedFollowerIds.add(UUID.fromString(followerId));
			} catch (IllegalArgumentException ignored) {
			}
		}
	}

	private String queuedRoutesAsString() {
		StringBuilder routes = new StringBuilder();
		for (QueuedRoute queuedRoute : this.queuedRoutes) {
			if (!routes.isEmpty()) {
				routes.append(';');
			}
			RouteTarget queuedTarget = queuedRoute.target();
			BlockPos junctionPos = queuedTarget.junctionPos();
			if (junctionPos == null) {
				routes.append('0');
			} else {
				routes.append('1');
				routes.append(',');
				routes.append(junctionPos.getX());
				routes.append(',');
				routes.append(junctionPos.getY());
				routes.append(',');
				routes.append(junctionPos.getZ());
			}
			routes.append(',');
			routes.append(queuedTarget.contactPos().getX());
			routes.append(',');
			routes.append(queuedTarget.contactPos().getY());
			routes.append(',');
			routes.append(queuedTarget.contactPos().getZ());
			routes.append(',');
			routes.append(queuedRoute.direction().ordinal());
		}
		return routes.toString();
	}

	private void readQueuedRoutes(String storedRoutes) {
		if (storedRoutes.isBlank()) {
			return;
		}
		for (String storedRoute : storedRoutes.split(";")) {
			String[] values = storedRoute.split(",");
			try {
				if (values.length == 5 && values[0].equals("0")) {
					BlockPos contactPos = new BlockPos(
						Integer.parseInt(values[1]),
						Integer.parseInt(values[2]),
						Integer.parseInt(values[3])
					);
					Direction routeDirection = Direction.values()[Math.floorMod(Integer.parseInt(values[4]), Direction.values().length)];
					this.queuedRoutes.add(new QueuedRoute(new RouteTarget(null, contactPos), routeDirection, List.of()));
					continue;
				}
				if (values.length == 8 && values[0].equals("1")) {
					BlockPos junctionPos = new BlockPos(
						Integer.parseInt(values[1]),
						Integer.parseInt(values[2]),
						Integer.parseInt(values[3])
					);
					BlockPos contactPos = new BlockPos(
						Integer.parseInt(values[4]),
						Integer.parseInt(values[5]),
						Integer.parseInt(values[6])
					);
					Direction routeDirection = Direction.values()[Math.floorMod(Integer.parseInt(values[7]), Direction.values().length)];
					this.queuedRoutes.add(new QueuedRoute(new RouteTarget(junctionPos, contactPos), routeDirection, List.of()));
					continue;
				}
				if (values.length == 7) {
					BlockPos junctionPos = new BlockPos(
						Integer.parseInt(values[0]),
						Integer.parseInt(values[1]),
						Integer.parseInt(values[2])
					);
					BlockPos contactPos = new BlockPos(
						Integer.parseInt(values[3]),
						Integer.parseInt(values[4]),
						Integer.parseInt(values[5])
					);
					Direction routeDirection = Direction.values()[Math.floorMod(Integer.parseInt(values[6]), Direction.values().length)];
					this.queuedRoutes.add(new QueuedRoute(new RouteTarget(junctionPos, contactPos), routeDirection, List.of()));
				}
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private boolean isLinkedFollower() {
		return this.linkLeaderId != null;
	}

	private boolean sharesMoverLink(MoverEntity other) {
		UUID thisLinkId = this.linkLeaderId == null ? this.getUUID() : this.linkLeaderId;
		UUID otherLinkId = other.linkLeaderId == null ? other.getUUID() : other.linkLeaderId;
		return thisLinkId.equals(otherLinkId);
	}

	private int getLinkedMoverCount() {
		return this.linkedFollowerIds.size();
	}

	private List<MoverEntity> getLinkedMovers(ServerLevel level) {
		List<MoverEntity> movers = new ArrayList<>();
		movers.add(this);
		for (UUID followerId : this.linkedFollowerIds) {
			Entity followerEntity = level.getEntity(followerId);
			if (followerEntity instanceof MoverEntity follower && !follower.isRemoved()) {
				movers.add(follower);
			}
		}
		return movers;
	}

	private record RouteTarget(BlockPos junctionPos, BlockPos contactPos) {
		private Vec3 position() {
			return new Vec3(contactPos.getX() + 0.5D, contactPos.getY(), contactPos.getZ() + 0.5D);
		}
	}

	private record RouteSignCandidate(RouteTarget route, int forwardDistance, int lateralDistance) {
		private boolean isCloserThan(RouteSignCandidate otherCandidate) {
			return forwardDistance < otherCandidate.forwardDistance()
				|| forwardDistance == otherCandidate.forwardDistance() && lateralDistance < otherCandidate.lateralDistance();
		}
	}

	private record RoutePlan(RouteTarget target, List<FlightAnchor> flightAnchors) {
	}

	private record QueuedRoute(RouteTarget target, Direction direction, List<FlightAnchor> flightAnchors) {
	}

	private record RouteAnchor(BlockPos pos, int range, boolean junction) {
	}

	private record AnchorSearchNode(RouteAnchor anchor, List<RouteAnchor> path) {
	}

	private record GridSearchNode(BlockPos pos, int distance) {
	}

	private record ReachableContact(BlockPos pos, int distance) {
	}

	private record LinkedContactCandidate(BlockPos leaderTargetPos, int contactDistance) {
	}

	private record FlightAnchor(BlockPos pos, int range) {
		private boolean contains(BlockPos targetPos) {
			return Math.abs(targetPos.getX() - pos.getX()) <= range
				&& Math.abs(targetPos.getY() - pos.getY()) <= range
				&& Math.abs(targetPos.getZ() - pos.getZ()) <= range;
		}
	}
}
