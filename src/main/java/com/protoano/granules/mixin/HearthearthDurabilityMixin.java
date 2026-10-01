package com.protoano.granules.mixin;

import com.protoano.granules.enchantment.Hearthearth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class HearthearthDurabilityMixin {
    @ModifyVariable(method = "applyDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int preserveLastDurability(int damage) {
        var stack = (ItemStack) (Object) this;
        if (Hearthearth.isSword(stack)) {
            return Math.min(damage, stack.getMaxDamage() - 1);
        }
        return damage;
    }

    @Inject(method = "canDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void preventExhaustedMining(BlockState state, Level level, BlockPos pos, Player player,
                                        CallbackInfoReturnable<Boolean> callback) {
        if (Hearthearth.isExhausted((ItemStack) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void stopExhaustedMiningProgress(BlockState state, CallbackInfoReturnable<Float> callback) {
        if (Hearthearth.isExhausted((ItemStack) (Object) this)) {
            callback.setReturnValue(0.0F);
        }
    }
}
