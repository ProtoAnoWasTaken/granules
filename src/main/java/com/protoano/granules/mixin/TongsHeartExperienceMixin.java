package com.protoano.granules.mixin;

import com.protoano.granules.item.GildedTongsItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreakingHeartBlock.class)
public abstract class TongsHeartExperienceMixin {
    @Inject(method = "tryAwardExperience", at = @At("HEAD"), cancellable = true)
    private void granules$preserveHeartWithoutExperience(Player player, BlockState state, Level level, BlockPos pos, CallbackInfo callback) {
        if (player.getMainHandItem().getItem() instanceof GildedTongsItem && state.is(GildedTongsItem.HARVESTABLE)) {
            callback.cancel();
        }
    }
}
