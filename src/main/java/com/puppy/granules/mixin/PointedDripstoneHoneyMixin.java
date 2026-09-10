package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneHoneyMixin {
	@Inject(method = "canFillCauldron", at = @At("RETURN"), cancellable = true)
	private static void granules$allowHoneyCauldronFilling(Fluid fluid, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (fluid == GranulesMod.HONEY) {
			callbackInfo.setReturnValue(true);
		}
	}

	@ModifyVariable(method = "maybeTransferFluid", at = @At(value = "STORE"), ordinal = 0)
	private static Fluid granules$scheduleHoneyCauldronTransfer(Fluid fluid) {
		if (fluid == GranulesMod.HONEY) {
			return Fluids.WATER;
		}
		return fluid;
	}

	@Redirect(
		method = "spawnDripParticle(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;getDripParticle(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/particles/ParticleOptions;"
		)
	)
	private static ParticleOptions granules$useHoneyDripParticle(Level level, Fluid fluid, BlockPos sourcePos) {
		if (fluid == GranulesMod.HONEY) {
			return ParticleTypes.DRIPPING_HONEY;
		}
		if (fluid == Fluids.LAVA) {
			return ParticleTypes.DRIPPING_DRIPSTONE_LAVA;
		}
		return ParticleTypes.DRIPPING_DRIPSTONE_WATER;
	}
}
