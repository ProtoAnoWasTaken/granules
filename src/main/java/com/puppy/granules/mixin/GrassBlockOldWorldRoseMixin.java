package com.puppy.granules.mixin;

import com.puppy.granules.world.OldWorldRoseBonemeal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrassBlock.class)
public abstract class GrassBlockOldWorldRoseMixin {
	@Inject(method = "performBonemeal", at = @At("TAIL"))
	private void granules$growOldWorldRose(
		ServerLevel level,
		RandomSource random,
		BlockPos pos,
		BlockState state,
		CallbackInfo callbackInfo
	) {
		OldWorldRoseBonemeal.tryGrow(level, random, pos);
	}
}
