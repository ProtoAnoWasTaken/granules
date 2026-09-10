package com.puppy.granules.fluid;

import com.puppy.granules.GranulesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class HoneyFluid extends FlowingFluid {
	@Override
	public Fluid getFlowing() {
		return GranulesMod.FLOWING_HONEY;
	}

	@Override
	public Fluid getSource() {
		return GranulesMod.HONEY;
	}

	@Override
	public Item getBucket() {
		return GranulesMod.HONEY_BUCKET;
	}

	@Override
	public @Nullable ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_HONEY;
	}

	@Override
	protected boolean canConvertToSource(ServerLevel level) {
		return false;
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel serverLevel) {
			Block.dropResources(state, serverLevel, pos);
		}
	}

	@Override
	public int getSlopeFindDistance(LevelReader level) {
		return 0;
	}

	@Override
	public BlockState createLegacyBlock(FluidState fluidState) {
		return GranulesMod.HONEY_FLUID_BLOCK.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, getLegacyLevel(fluidState));
	}

	@Override
	public boolean isSame(Fluid other) {
		return other == GranulesMod.HONEY || other == GranulesMod.FLOWING_HONEY;
	}

	@Override
	public int getDropOff(LevelReader level) {
		return 4;
	}

	@Override
	public int getTickDelay(LevelReader level) {
		return 100;
	}

	@Override
	public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
		return direction == Direction.DOWN && !other.is(FluidTags.WATER);
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	public static class Flowing extends HoneyFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState fluidState) {
			return fluidState.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return false;
		}
	}

	public static class Source extends HoneyFluid {
		@Override
		public int getAmount(FluidState fluidState) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return true;
		}
	}
}
