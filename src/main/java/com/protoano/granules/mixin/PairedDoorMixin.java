package com.protoano.granules.mixin;

import com.protoano.granules.world.PairedDoors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class PairedDoorMixin {
    @Inject(method = "useWithoutItem", at = @At("RETURN"))
    private void granules$togglePartner(BlockState state, Level level, BlockPos pos, Player player,
        BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (!level.isClientSide() && callback.getReturnValue() instanceof InteractionResult.Success) {
            PairedDoors.synchronize(level, pos, player);
        }
    }
}
