package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.protoano.granules.entity.FletchersArrowEntity;
import com.protoano.granules.fletching.ArrowParts;
import com.protoano.granules.fletching.ResinClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
public abstract class ResinArrowCollisionMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;clipIncludingBorder(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult granules$traceThroughLeaves(Level level, ClipContext context, Operation<BlockHitResult> original) {
        if ((Object) this instanceof FletchersArrowEntity arrow && arrow.parts().head() == ArrowParts.Head.RESIN) {
            return original.call(level, new ResinClipContext(context, arrow));
        }
        return original.call(level, context);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape granules$avoidEmbeddingInLeaves(BlockState state, BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        if ((Object) this instanceof FletchersArrowEntity arrow && arrow.parts().head() == ArrowParts.Head.RESIN && state.is(BlockTags.LEAVES)) {
            return Shapes.empty();
        }
        return original.call(state, level, pos);
    }
}
