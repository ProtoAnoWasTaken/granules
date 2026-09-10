package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyloggingProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemHoneyloggingMixin {
	@Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
	private void granules$honeylogPlacedBlocks(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callbackInfo) {
		BlockState placementState = callbackInfo.getReturnValue();
		if (
			placementState != null
				&& placementState.hasProperty(HoneyloggingProperties.HONEYLOGGED)
				&& context.getLevel().getFluidState(context.getClickedPos()).is(GranulesMod.HONEY_TAG)
		) {
			callbackInfo.setReturnValue(placementState.setValue(HoneyloggingProperties.HONEYLOGGED, true));
		}
	}
}
