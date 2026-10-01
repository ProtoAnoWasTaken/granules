package com.protoano.granules.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.protoano.granules.world.FeatureLocations;
import com.protoano.granules.world.FeatureLocator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public final class LocateFeatureCommand {
    private LocateFeatureCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registries) {
        dispatcher.register(Commands.literal("locate")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("feature")
                .then(Commands.argument("feature", ResourceKeyArgument.key(Registries.CONFIGURED_FEATURE))
                    .executes(context -> locate(context, 2048))
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1, 8192))
                        .executes(context -> locate(context, IntegerArgumentType.getInteger(context, "radius")))))));
    }

    private static int locate(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Identifier feature = ResourceKeyArgument.getConfiguredFeature(context, "feature").key().identifier();
        BlockPos origin = BlockPos.containing(source.getPosition());
        if (feature.equals(FeatureLocator.DESERT_WELL) || feature.equals(FeatureLocator.BADLANDS_WELL)) {
            FeatureLocator.scanNearbyWells(level, origin, radius);
        }
        FeatureLocator.flush(level);
        var result = FeatureLocations.get(level).nearest(feature, origin, radius);
        if (result.isEmpty()) {
            source.sendFailure(Component.literal("No recorded " + feature + " within " + radius
                + " blocks in this dimension. Features are recorded as chunks generate; older wells are also detected in loaded chunks."));
            return 0;
        }
        BlockPos position = result.get();
        int distance = (int) Math.sqrt(FeatureLocations.horizontalDistanceSquared(origin, position));
        String coordinates = position.getX() + " " + position.getY() + " " + position.getZ();
        Component link = Component.literal("[" + position.getX() + ", " + position.getY() + ", " + position.getZ() + "]")
            .withStyle(style -> style.withColor(ChatFormatting.GREEN).withUnderlined(true)
                .withClickEvent(new ClickEvent.SuggestCommand("/execute in " + level.dimension().identifier()
                    + " run tp @s " + coordinates)));
        source.sendSuccess(() -> Component.literal("Nearest recorded " + feature + " is at ")
            .append(link).append(" (" + distance + " blocks away)."), false);
        return distance;
    }
}
