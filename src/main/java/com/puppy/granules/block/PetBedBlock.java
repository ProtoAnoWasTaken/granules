package com.puppy.granules.block;

import com.mojang.serialization.MapCodec;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.item.PetBedItem;
import com.puppy.granules.pet.PetBedMenu;
import com.puppy.granules.pet.PetBedService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PetBedBlock extends Block implements SimpleWaterloggedBlock {
	public static final MapCodec<PetBedBlock> CODEC = simpleCodec(PetBedBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final EnumProperty<PetBedWood> WOOD = EnumProperty.create("wood", PetBedWood.class);
	public static final EnumProperty<DyeColor> WOOL = EnumProperty.create("wool", DyeColor.class);
	private static final VoxelShape NORTH_SHAPE = Shapes.or(
		Block.box(0.0D, 0.0D, 0.0D, 15.0D, 1.0D, 15.0D),
		Block.box(0.0D, 0.0D, 15.0D, 16.0D, 2.0D, 16.0D),
		Block.box(15.0D, 0.0D, 0.0D, 16.0D, 2.0D, 15.0D)
	);
	private static final VoxelShape EAST_SHAPE = Shapes.or(
		Block.box(1.0D, 0.0D, 0.0D, 16.0D, 1.0D, 15.0D),
		Block.box(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 16.0D),
		Block.box(1.0D, 0.0D, 15.0D, 16.0D, 2.0D, 16.0D)
	);
	private static final VoxelShape SOUTH_SHAPE = Shapes.or(
		Block.box(1.0D, 0.0D, 1.0D, 16.0D, 1.0D, 16.0D),
		Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 1.0D),
		Block.box(0.0D, 0.0D, 1.0D, 1.0D, 2.0D, 16.0D)
	);
	private static final VoxelShape WEST_SHAPE = Shapes.or(
		Block.box(0.0D, 0.0D, 1.0D, 15.0D, 1.0D, 16.0D),
		Block.box(15.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
		Block.box(0.0D, 0.0D, 0.0D, 15.0D, 2.0D, 1.0D)
	);

	public PetBedBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(
			stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(WATERLOGGED, false)
				.setValue(WOOD, PetBedWood.OAK)
				.setValue(WOOL, DyeColor.WHITE)
		);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED, WOOD, WOOL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(WATERLOGGED, fluidState.is(FluidTags.WATER));
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
		if (state.getValue(WATERLOGGED)) {
			scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return state;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		if (state.getValue(WATERLOGGED)) {
			return Fluids.WATER.getSource(false);
		}
		return super.getFluidState(state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state);
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state);
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		if (level instanceof ServerLevel serverLevel && fallDistance > 0.0D) {
			serverLevel.sendParticles(
				new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WOOL.pick(state.getValue(WOOL)).defaultBlockState()),
				entity.getX(),
				pos.getY() + 0.25D,
				entity.getZ(),
				12,
				0.35D,
				0.05D,
				0.35D,
				0.02D
			);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult
	) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		serverPlayer.openMenu(PetBedMenu.provider(level, pos));
		level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
		return InteractionResult.CONSUME;
	}

	@Override
	protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		return List.of(PetBedItem.createStack(state.getValue(WOOD), state.getValue(WOOL)));
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return PetBedItem.createStack(state.getValue(WOOD), state.getValue(WOOL));
	}

	private static VoxelShape shapeFor(BlockState state) {
		return switch (state.getValue(FACING)) {
			case NORTH -> NORTH_SHAPE;
			case EAST -> EAST_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case WEST -> WEST_SHAPE;
			default -> NORTH_SHAPE;
		};
	}
}
