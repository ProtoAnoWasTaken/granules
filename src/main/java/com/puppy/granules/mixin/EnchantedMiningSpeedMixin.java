package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class EnchantedMiningSpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void applySlack(BlockState state, CallbackInfoReturnable<Float> callback) {
        Player self = (Player) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        int enchantmentLevel = GranulesEnchantments.level(level, self.getMainHandItem(), GranulesEnchantments.CURSE_OF_SLACK);
        float penalty = switch (enchantmentLevel) {
            case 1 -> 2.0F;
            case 2 -> 5.0F;
            case 3 -> 10.0F;
            case 4 -> 15.0F;
            case 5 -> 20.0F;
            default -> 0.0F;
        };
        callback.setReturnValue(Math.max(0.0F, callback.getReturnValue() - penalty));
    }
}
