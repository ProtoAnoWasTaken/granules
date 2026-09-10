package com.puppy.granules.mixin;

import com.puppy.granules.world.CampfireTanning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
	@Inject(method = "cooldownTick", at = @At("TAIL"))
	private static void tanDuringDaytime(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo callbackInfo) {
		CampfireTanning.tick(level, pos, state, campfire);
	}
}
