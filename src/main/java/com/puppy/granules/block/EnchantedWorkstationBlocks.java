package com.puppy.granules.block;

import com.puppy.granules.world.EnchantedCauldronProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class EnchantedWorkstationBlocks {
	private static final int RUNE_PARTICLE_INTERVAL = 2;

	private EnchantedWorkstationBlocks() {
	}

	public static class EnchantedAnvil extends AnvilBlock implements EntityBlock {
		public EnchantedAnvil(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void onLand(Level level, BlockPos pos, BlockState fallingState, BlockState landedState, FallingBlockEntity fallingBlock) {
		}

		@Override
		public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}
	}

	public static class EnchantedEnchantingTable extends EnchantingTableBlock {
		public EnchantedEnchantingTable(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}
	}

	public static class EnchantedSmithingTable extends SmithingTableBlock implements EntityBlock {
		public EnchantedSmithingTable(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}
	}

	public static class EnchantedCraftingTable extends CraftingTableBlock implements EntityBlock {
		public EnchantedCraftingTable(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}
	}

	public static class EnchantedFletchingTable extends Block implements EntityBlock {
		public EnchantedFletchingTable(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}
	}

	public static class EnchantedStonecutter extends StonecutterBlock implements EntityBlock {
		public EnchantedStonecutter(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}
	}

	public static class EnchantedGrindstone extends GrindstoneBlock implements EntityBlock {
		public EnchantedGrindstone(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}
	}

	public static class EnchantedCauldron extends CauldronBlock implements EntityBlock {
		public EnchantedCauldron(BlockBehaviour.Properties properties) {
			super(properties);
		}

		@Override
		public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
			super.animateTick(state, level, pos, random);
			spawnRuneDrip(level, pos, random);
		}

		@Override
		public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
			return new EnchantedWorkstationBlockEntity(pos, state);
		}

		@Override
		public boolean isFull(BlockState state) {
			EnchantedCauldronProperties.Content content = state.getValue(EnchantedCauldronProperties.CONTENT);
			int level = state.getValue(EnchantedCauldronProperties.LEVEL);
			if (content == EnchantedCauldronProperties.Content.LAVA) {
				return level == 3;
			}
			return content != EnchantedCauldronProperties.Content.EMPTY && level == 9;
		}

		@Override
		protected boolean canReceiveStalactiteDrip(Fluid fluid) {
			return fluid == Fluids.WATER || fluid == Fluids.LAVA;
		}

		@Override
		protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
			if (fluid == Fluids.WATER && canAdd(state, EnchantedCauldronProperties.Content.WATER, 1, 9)) {
				level.setBlockAndUpdate(pos, withContent(state, EnchantedCauldronProperties.Content.WATER, getLevel(state) + 1));
			}
			if (fluid == Fluids.LAVA && canAdd(state, EnchantedCauldronProperties.Content.LAVA, 1, 3)) {
				level.setBlockAndUpdate(pos, withContent(state, EnchantedCauldronProperties.Content.LAVA, getLevel(state) + 1));
			}
		}

		private static boolean canAdd(BlockState state, EnchantedCauldronProperties.Content content, int amount, int maximum) {
			EnchantedCauldronProperties.Content currentContent = state.getValue(EnchantedCauldronProperties.CONTENT);
			return (currentContent == EnchantedCauldronProperties.Content.EMPTY || currentContent == content) && getLevel(state) + amount <= maximum;
		}

		private static int getLevel(BlockState state) {
			return state.getValue(EnchantedCauldronProperties.LEVEL);
		}

		private static BlockState withContent(BlockState state, EnchantedCauldronProperties.Content content, int level) {
			return state.setValue(EnchantedCauldronProperties.CONTENT, content).setValue(EnchantedCauldronProperties.LEVEL, level);
		}
	}

	private static void spawnRuneDrip(Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(RUNE_PARTICLE_INTERVAL) != 0) {
			return;
		}
		level.addParticle(
			ParticleTypes.ENCHANT,
			pos.getX() + random.nextDouble(),
			pos.getY() + 0.8D,
			pos.getZ() + random.nextDouble(),
			0.0D,
			0.02D,
			0.0D
		);
	}
}
