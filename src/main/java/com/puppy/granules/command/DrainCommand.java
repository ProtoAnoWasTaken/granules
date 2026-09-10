package com.puppy.granules.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyloggingProperties;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

public final class DrainCommand {
	private static final int MAXIMUM_RADIUS = 64;

	private DrainCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		dispatcher.register(
			Commands.literal("drain")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(
					Commands.argument("radius", IntegerArgumentType.integer(0, MAXIMUM_RADIUS))
						.then(
							Commands.argument("fluid", ResourceArgument.resource(registryAccess, Registries.FLUID))
								.then(
									Commands.argument("source_blocks", BoolArgumentType.bool())
										.then(
											Commands.argument("flowing_blocks", BoolArgumentType.bool())
												.then(
													Commands.argument("fluidlogged_blocks", BoolArgumentType.bool())
														.executes(DrainCommand::drain)
												)
										)
								)
							)
					)
		);
	}

	private static int drain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		int radius = IntegerArgumentType.getInteger(context, "radius");
		Holder.Reference<Fluid> fluidHolder = ResourceArgument.getResource(context, "fluid", Registries.FLUID);
		Fluid targetFluid = fluidHolder.value();
		boolean drainSources = BoolArgumentType.getBool(context, "source_blocks");
		boolean drainFlowing = BoolArgumentType.getBool(context, "flowing_blocks");
		boolean drainFluidlogged = BoolArgumentType.getBool(context, "fluidlogged_blocks");
		ServerLevel level = source.getLevel();
		BlockPos center = BlockPos.containing(source.getPosition());
		long radiusSquared = (long)radius * radius;
		int minimumY = Math.max(level.getMinY(), center.getY() - radius);
		int maximumY = Math.min(level.getMaxY() - 1, center.getY() + radius);
		List<BlockPos> fluidPositions = new ArrayList<>();
		List<BlockPos> fluidloggedPositions = new ArrayList<>();
		int modificationLimit = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS);

		for (int y = minimumY; y <= maximumY; y++) {
			int yDistance = y - center.getY();
			for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
				int xDistance = x - center.getX();
				for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
					int zDistance = z - center.getZ();
					if ((long)xDistance * xDistance + (long)yDistance * yDistance + (long)zDistance * zDistance > radiusSquared) {
						continue;
					}

					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (drainFluidlogged && isFluidloggedWith(state, targetFluid)) {
						fluidloggedPositions.add(pos);
					} else if (shouldDrainFluidBlock(state.getFluidState(), targetFluid, drainSources, drainFlowing)) {
						fluidPositions.add(pos);
					}

					if (fluidPositions.size() + fluidloggedPositions.size() > modificationLimit) {
						source.sendFailure(Component.literal("The selected drain area would modify more blocks than the current command limit allows."));
						return 0;
					}
				}
			}
		}

		for (BlockPos pos : fluidPositions) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}

		for (BlockPos pos : fluidloggedPositions) {
			BlockState state = level.getBlockState(pos);
			if (isFluidloggedWith(state, targetFluid)) {
				level.setBlock(pos, clearFluidloggedState(state, targetFluid), 3);
			}
		}

		int drained = fluidPositions.size() + fluidloggedPositions.size();
		source.sendSuccess(() -> Component.literal("Drained " + drained + " block" + (drained == 1 ? "" : "s") + " of " + fluidHolder.key().identifier()), true);
		return drained;
	}

	private static boolean shouldDrainFluidBlock(FluidState fluidState, Fluid targetFluid, boolean drainSources, boolean drainFlowing) {
		if (!targetFluid.isSame(fluidState.getType())) {
			return false;
		}
		return fluidState.isSource() ? drainSources : drainFlowing;
	}

	private static boolean isFluidloggedWith(BlockState state, Fluid targetFluid) {
		if (targetFluid.isSame(Fluids.WATER) && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			return state.getValue(BlockStateProperties.WATERLOGGED);
		}
		if (targetFluid.isSame(GranulesMod.HONEY) && state.hasProperty(HoneyloggingProperties.HONEYLOGGED)) {
			return state.getValue(HoneyloggingProperties.HONEYLOGGED);
		}
		return false;
	}

	private static BlockState clearFluidloggedState(BlockState state, Fluid targetFluid) {
		if (targetFluid.isSame(Fluids.WATER)) {
			return state.setValue(BlockStateProperties.WATERLOGGED, false);
		}
		return state.setValue(HoneyloggingProperties.HONEYLOGGED, false);
	}
}
