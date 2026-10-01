package com.protoano.granules.mixin;

import com.protoano.granules.item.IronLockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DoorBlock.class, TrapDoorBlock.class})
public abstract class IronLockBlockMixin {
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void granules$addInversion(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo callback) {
        if (IronLockItem.supports((Block) (Object) this)) {
            builder.add(IronLockItem.INVERTED);
        }
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void granules$invertedPower(BlockState state, Level level, BlockPos pos, Block neighbor, Orientation orientation, boolean piston, CallbackInfo callback) {
        if (state.hasProperty(IronLockItem.INVERTED) && state.getValue(IronLockItem.INVERTED)) {
            if (!level.isClientSide()) {
                IronLockItem.update(level, pos, state, true, null);
            }
            callback.cancel();
        }
    }
}
