package com.protoano.granules.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class PairedDoors {
    private PairedDoors() {
    }

    public static void synchronize(Level level, BlockPos clicked, Player player) {
        BlockState state = level.getBlockState(clicked);
        if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()) {
            return;
        }
        BlockPos base = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? clicked.below() : clicked;
        Direction facing = state.getValue(DoorBlock.FACING);
        Direction side = state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
            ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos partnerPos = base.relative(side);
        BlockState partner = level.getBlockState(partnerPos);
        if (partner.getBlock() instanceof DoorBlock other && other.type().canOpenByHand()
            && partner.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
            && partner.getValue(DoorBlock.FACING) == facing
            && partner.getValue(DoorBlock.HINGE) != state.getValue(DoorBlock.HINGE)
            && level.mayInteract(player, partnerPos)) {
            other.setOpen(player, level, partner, partnerPos, state.getValue(DoorBlock.OPEN));
        }
    }
}
