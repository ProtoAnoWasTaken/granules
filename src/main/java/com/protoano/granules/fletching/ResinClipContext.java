package com.protoano.granules.fletching;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ResinClipContext extends ClipContext {
    public ResinClipContext(ClipContext original, Entity arrow) {
        super(original.getFrom(), original.getTo(), Block.COLLIDER, Fluid.NONE, arrow);
    }

    @Override
    public VoxelShape getBlockShape(BlockState state, BlockGetter getter, BlockPos pos) {
        if (state.is(BlockTags.LEAVES)) {
            return Shapes.empty();
        }
        return super.getBlockShape(state, getter, pos);
    }
}
