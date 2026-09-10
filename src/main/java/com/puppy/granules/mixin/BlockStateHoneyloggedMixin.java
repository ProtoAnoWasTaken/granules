package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyloggingProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateHoneyloggedMixin {
	@Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
	private void granules$provideHoneyFluid(CallbackInfoReturnable<FluidState> callbackInfo) {
		BlockState state = (BlockState) (Object) this;
		if (state.hasProperty(HoneyloggingProperties.HONEYLOGGED) && state.getValue(HoneyloggingProperties.HONEYLOGGED)) {
			callbackInfo.setReturnValue(GranulesMod.HONEY.defaultFluidState());
		}
	}
}
