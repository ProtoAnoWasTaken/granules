package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
public abstract class EnchantedBlockBreakingMixin {
    @Shadow
    protected ServerPlayer player;

    @Unique
    private BlockState granules$originalState;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void captureOriginalBlock(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        granules$originalState = player.level().getBlockState(pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void breakEnchantedArea(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && granules$originalState != null) {
            GranulesEnchantments.breakAdditionalBlocks(player, pos, granules$originalState);
        }
    }
}
