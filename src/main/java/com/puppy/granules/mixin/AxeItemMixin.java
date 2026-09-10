package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
public abstract class AxeItemMixin {
	@Inject(method = "evaluateNewBlockState", at = @At("HEAD"), cancellable = true)
	private void granules$deglowWool(
		Level level,
		BlockPos pos,
		Player player,
		BlockState state,
		CallbackInfoReturnable<Optional<BlockState>> callbackInfo
	) {
		Block woolBlock = GranulesMod.getVanillaWoolBlock(state.getBlock());
		if (woolBlock != null) {
			callbackInfo.setReturnValue(Optional.of(woolBlock.defaultBlockState()));
		}
	}
}
