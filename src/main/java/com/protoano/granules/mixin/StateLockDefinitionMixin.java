package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.protoano.granules.item.StateLocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class StateLockDefinitionMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"))
    private void granules$addLock(Block block, StateDefinition.Builder<Block, BlockState> builder, Operation<Void> original) {
        original.call(block, builder);
        if (StateLocks.supports(block)) {
            builder.add(StateLocks.LOCKED);
        }
    }
}
