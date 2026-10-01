package com.protoano.granules.mixin;

import com.protoano.granules.block.FenceWallOrientation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossCollisionBlock.class)
public abstract class FenceShapeMixin {
    @Inject(method = {"getShape", "getCollisionShape"}, at = @At("RETURN"), cancellable = true)
    private void granules$rotateShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context,
        CallbackInfoReturnable<VoxelShape> callback) {
        if (state.hasProperty(FenceWallOrientation.AXIS)) {
            callback.setReturnValue(FenceWallOrientation.rotateShape(callback.getReturnValue(), state.getValue(FenceWallOrientation.AXIS)));
        }
    }
}
