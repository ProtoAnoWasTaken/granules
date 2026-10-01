package com.protoano.granules.block;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.phys.Vec3;

public final class FenceLeashOrientation {
    private FenceLeashOrientation() {
    }

    public static Direction.Axis axis(LeashFenceKnotEntity knot) {
        var state = knot.level().getBlockState(knot.getPos());
        if (state.getBlock() instanceof FenceBlock && state.hasProperty(FenceWallOrientation.AXIS)) {
            return state.getValue(FenceWallOrientation.AXIS);
        }
        return Direction.Axis.Y;
    }

    public static Vec3 rotate(Vec3 vector, Direction.Axis axis) {
        return switch (axis) {
            case X -> new Vec3(vector.y, -vector.x, vector.z);
            case Z -> new Vec3(vector.x, -vector.z, vector.y);
            case Y -> vector;
        };
    }

    public static Vec3 rotatePosition(LeashFenceKnotEntity knot, Vec3 position) {
        Vec3 center = Vec3.atCenterOf(knot.getPos());
        return center.add(rotate(position.subtract(center), axis(knot)));
    }
}
