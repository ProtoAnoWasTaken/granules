package com.protoano.granules.block;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class FenceWallOrientation {
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create(
        "granules_axis",
        Direction.Axis.class,
        List.of(Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z)
    );

    private FenceWallOrientation() {
    }

    public static boolean matches(BlockState state, Direction.Axis axis) {
        return (state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock)
            && state.hasProperty(AXIS)
            && state.getValue(AXIS) == axis;
    }

    public static Direction toWorld(Direction direction, Direction.Axis axis) {
        if (direction == null) {
            return null;
        }
        if (axis == Direction.Axis.X) {
            return switch (direction) {
                case UP -> Direction.EAST;
                case DOWN -> Direction.WEST;
                case EAST -> Direction.DOWN;
                case WEST -> Direction.UP;
                default -> direction;
            };
        }
        if (axis == Direction.Axis.Z) {
            return switch (direction) {
                case UP -> Direction.SOUTH;
                case DOWN -> Direction.NORTH;
                case SOUTH -> Direction.DOWN;
                case NORTH -> Direction.UP;
                default -> direction;
            };
        }
        return direction;
    }

    public static Direction toLocal(Direction direction, Direction.Axis axis) {
        for (Direction candidate : Direction.values()) {
            if (toWorld(candidate, axis) == direction) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean connects(BlockState state, BlockGetter level, BlockPos position, Direction localDirection) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction direction = toWorld(localDirection, axis);
        BlockPos neighborPosition = position.relative(direction);
        BlockState neighbor = level.getBlockState(neighborPosition);
        if (neighbor.getBlock() instanceof FenceBlock || neighbor.getBlock() instanceof WallBlock) {
            return matches(neighbor, axis);
        }
        if (neighbor.getBlock() instanceof FenceGateBlock) {
            return axis == Direction.Axis.Y && FenceGateBlock.connectsToDirection(neighbor, direction);
        }
        if (state.getBlock() instanceof WallBlock && neighbor.getBlock() instanceof IronBarsBlock) {
            return axis == Direction.Axis.Y;
        }
        return !Block.isExceptionForConnection(neighbor)
            && neighbor.isFaceSturdy(level, neighborPosition, direction.getOpposite());
    }

    public static VoxelShape rotateShape(VoxelShape shape, Direction.Axis axis) {
        if (axis == Direction.Axis.Y || shape.isEmpty()) {
            return shape;
        }
        VoxelShape result = Shapes.empty();
        for (net.minecraft.world.phys.AABB box : shape.toAabbs()) {
            VoxelShape rotated;
            if (axis == Direction.Axis.X) {
                rotated = Shapes.box(box.minY, 1.0 - box.maxX, box.minZ, box.maxY, 1.0 - box.minX, box.maxZ);
            } else {
                rotated = Shapes.box(box.minX, 1.0 - box.maxZ, box.minY, box.maxX, 1.0 - box.minZ, box.maxY);
            }
            result = Shapes.or(result, rotated);
        }
        return result;
    }

    public static VoxelShape unrotateShape(VoxelShape shape, Direction.Axis axis) {
        VoxelShape result = shape;
        for (int turn = 0; turn < 3; turn++) {
            result = rotateShape(result, axis);
        }
        return result;
    }
}
