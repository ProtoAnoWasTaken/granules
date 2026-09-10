package com.puppy.granules.block;

import java.util.Comparator;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CasterBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	public static final IntegerProperty OUTPUT_POWER = IntegerProperty.create("output_power", 0, 15);

	public CasterBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(TRIGGERED, false).setValue(POWER, 0).setValue(OUTPUT_POWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, TRIGGERED, POWER, OUTPUT_POWER);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getNearestLookingDirection();
		return defaultBlockState()
			.setValue(FACING, facing)
			.setValue(POWERED, false)
			.setValue(TRIGGERED, false)
			.setValue(POWER, 0)
			.setValue(OUTPUT_POWER, 0);
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
		if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
			updateRearInput(state, level, pos);
		}
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
		if (!level.isClientSide()) {
			updateRearInput(state, level, pos);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		Direction facing = state.getValue(FACING);
		int input = getRearSignal(level, pos, facing);
		if (input == 0) {
			BlockState inactiveState = state.setValue(TRIGGERED, false).setValue(POWERED, false).setValue(POWER, 0).setValue(OUTPUT_POWER, 0);
			if (inactiveState.equals(state)) {
				return;
			}
			level.setBlock(pos, inactiveState, Block.UPDATE_CLIENTS);
			level.updateNeighborsAt(pos, this);
			return;
		}
		spawnSneakParticles(level, pos, facing, input, random);
		Entity target = findTarget(level, pos, facing, input);
		int output = target == null ? 0 : getOutputStrength(pos, facing, target, input);
		BlockState updatedState = state
			.setValue(TRIGGERED, true)
			.setValue(POWERED, output > 0)
			.setValue(POWER, input)
			.setValue(OUTPUT_POWER, output);
		if (!updatedState.equals(state)) {
			level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS);
			level.updateNeighborsAt(pos, this);
		}
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
		return getOutputSignal(state, direction);
	}

	@Override
	protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
		return getOutputSignal(state, direction);
	}

	private void updateRearInput(BlockState state, Level level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		int input = getRearSignal(level, pos, facing);
		if (input == 0) {
			BlockState inactiveState = state.setValue(TRIGGERED, false).setValue(POWERED, false).setValue(POWER, 0).setValue(OUTPUT_POWER, 0);
			if (!inactiveState.equals(state)) {
				level.setBlock(pos, inactiveState, Block.UPDATE_CLIENTS);
				level.updateNeighborsAt(pos, this);
			}
			return;
		}
		if (state.getValue(TRIGGERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(TRIGGERED, true), Block.UPDATE_CLIENTS);
		level.scheduleTick(pos, this, 1);
	}

	private Entity findTarget(Level level, BlockPos pos, Direction facing, int range) {
		Vec3 start = getRayStart(pos, facing);
		Vec3 rayEnd = getRayEnd(level, pos, facing, range);
		return level.getEntitiesOfClass(
			Entity.class,
			new AABB(start, rayEnd).inflate(0.1D),
			CasterBlock::isCasterTarget
		).stream()
			.filter(entity -> intersectsRay(entity, start, rayEnd))
			.min(Comparator.comparingDouble(entity -> getRayHitDistance(entity, start, rayEnd)))
			.orElse(null);
	}

	private static boolean isCasterTarget(Entity entity) {
		return entity.isAlive() && (
			entity instanceof ItemEntity
				|| entity instanceof Projectile
				|| entity instanceof Mob
				|| entity instanceof Player player && !player.isSpectator()
		);
	}

	private static boolean intersectsRay(Entity entity, Vec3 start, Vec3 end) {
		return entity.getBoundingBox().inflate(0.1D).clip(start, end).isPresent();
	}

	private static double getRayHitDistance(Entity entity, Vec3 start, Vec3 end) {
		Optional<Vec3> hit = entity.getBoundingBox().inflate(0.1D).clip(start, end);
		return hit.map(point -> point.distanceTo(start)).orElse(Double.MAX_VALUE);
	}

	private static int getTargetDistance(BlockPos pos, Direction facing, Entity target) {
		double delta;
		switch (facing.getAxis()) {
			case X -> delta = Math.abs(target.getX() - (pos.getX() + 0.5D));
			case Y -> delta = Math.abs(target.getY() - (pos.getY() + 0.5D));
			case Z -> delta = Math.abs(target.getZ() - (pos.getZ() + 0.5D));
			default -> throw new IllegalStateException("Unexpected direction axis");
		}
		return Math.max(1, (int) Math.ceil(delta));
	}

	private static int getOutputStrength(BlockPos pos, Direction facing, Entity target, int input) {
		int distance = getTargetDistance(pos, facing, target);
		return (int) Math.ceil(15.0D * (input - distance + 1) / input);
	}

	public static Vec3 getRayStart(BlockPos pos, Direction facing) {
		return Vec3.atCenterOf(pos).add(facing.getStepX() * 0.499D, facing.getStepY() * 0.499D, facing.getStepZ() * 0.499D);
	}

	public static Vec3 getRayEnd(Level level, BlockPos pos, Direction facing, int range) {
		Vec3 start = getRayStart(pos, facing);
		double effectiveRange = range;
		boolean waterAttenuated = false;
		boolean lavaAttenuated = false;
		for (int distance = 1; distance <= range; distance++) {
			if (distance > effectiveRange) {
				break;
			}
			BlockPos rayPos = pos.relative(facing, distance);
			BlockState rayState = level.getBlockState(rayPos);
			if (rayState.isSolidRender()) {
				return start.add(
					facing.getStepX() * (distance - 1),
					facing.getStepY() * (distance - 1),
					facing.getStepZ() * (distance - 1)
				);
			}
			FluidState fluidState = rayState.getFluidState();
			if (fluidState.is(Fluids.LAVA) && !lavaAttenuated) {
				effectiveRange = attenuateRange(effectiveRange, distance, 0.25D);
				lavaAttenuated = true;
			} else if (fluidState.is(Fluids.WATER) && !waterAttenuated) {
				effectiveRange = attenuateRange(effectiveRange, distance, 0.75D);
				waterAttenuated = true;
			}
			if (distance >= effectiveRange) {
				break;
			}
		}
		return start.add(
			facing.getStepX() * effectiveRange,
			facing.getStepY() * effectiveRange,
			facing.getStepZ() * effectiveRange
		);
	}

	private static double attenuateRange(double effectiveRange, int fluidDistance, double attenuation) {
		double distanceBeforeFluid = fluidDistance - 1;
		return distanceBeforeFluid + (effectiveRange - distanceBeforeFluid) * attenuation;
	}

	private static void spawnSneakParticles(ServerLevel level, BlockPos pos, Direction facing, int range, RandomSource random) {
		Vec3 start = getRayStart(pos, facing);
		Vec3 end = getRayEnd(level, pos, facing, range);
		Vec3 ray = end.subtract(start);
		if (ray.lengthSqr() <= 0.0025D) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!player.isShiftKeyDown() || player.distanceToSqr(Vec3.atCenterOf(pos)) > 1024.0D) {
				continue;
			}
			for (int particle = 0; particle < 4; particle++) {
				Vec3 particlePos = start.add(ray.scale(random.nextDouble()));
				level.sendParticles(
					player,
					DustParticleOptions.REDSTONE,
					false,
					false,
					particlePos.x,
					particlePos.y,
					particlePos.z,
					1,
					0.0D,
					0.0D,
					0.0D,
					0.0D
				);
			}
		}
	}

	private static int getRearSignal(LevelReader level, BlockPos pos, Direction facing) {
		return level.getSignal(pos.relative(facing.getOpposite()), facing);
	}

	private static int getOutputSignal(BlockState state, Direction direction) {
		if (!state.getValue(POWERED) || direction.getAxis() == state.getValue(FACING).getAxis()) {
			return 0;
		}
		return state.getValue(OUTPUT_POWER);
	}
}
