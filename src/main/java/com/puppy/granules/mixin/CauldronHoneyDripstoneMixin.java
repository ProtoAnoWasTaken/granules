package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyCauldrons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CauldronBlock.class)
public abstract class CauldronHoneyDripstoneMixin {
	@Inject(method = "receiveStalactiteDrip", at = @At("HEAD"), cancellable = true)
	private void granules$receiveHoneyDrip(BlockState state, Level level, BlockPos pos, Fluid fluid, CallbackInfo callbackInfo) {
		if (fluid != GranulesMod.HONEY || HoneyCauldrons.isFull(state)) {
			return;
		}

		BlockState newState = HoneyCauldrons.withHoneyLevel(state, HoneyCauldrons.getHoneyLevel(state) + 1);
		level.setBlockAndUpdate(pos, newState);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
		callbackInfo.cancel();
	}
}
