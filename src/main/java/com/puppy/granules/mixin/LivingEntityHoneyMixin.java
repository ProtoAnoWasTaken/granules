package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHoneyMixin {
	@Inject(method = "shouldTravelInFluid", at = @At("RETURN"), cancellable = true)
	private void granules$travelInHoney(CallbackInfoReturnable<Boolean> callbackInfo) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!callbackInfo.getReturnValue() && entity.isAffectedByFluids() && entity.getFluidHeight(GranulesMod.HONEY_TAG) > 0.0D) {
			callbackInfo.setReturnValue(true);
		}
	}

	@Redirect(
		method = "travelInFluid",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z")
	)
	private boolean granules$treatHoneyAsSwimmableWater(LivingEntity entity) {
		return entity.isInWater() || entity.getFluidHeight(GranulesMod.HONEY_TAG) > 0.0D;
	}

	@Redirect(
		method = "travelInWater",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V")
	)
	private void granules$slowHoneySwimming(LivingEntity entity, float speed, Vec3 input) {
		if (entity.getFluidHeight(GranulesMod.HONEY_TAG) > 0.0D) {
			entity.moveRelative(speed * 0.5F, input);
			return;
		}
		entity.moveRelative(speed, input);
	}

	@Redirect(
		method = "baseTick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z")
	)
	private boolean granules$treatHoneyAsDrownableFluid(LivingEntity entity, TagKey<Fluid> fluidTag) {
		return entity.isEyeInFluid(fluidTag) || (fluidTag == FluidTags.WATER && entity.isEyeInFluid(GranulesMod.HONEY_TAG));
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void granules$regenerateInHoney(CallbackInfo callbackInfo) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.level() instanceof ServerLevel && entity.getFluidHeight(GranulesMod.HONEY_TAG) >= 0.8D) {
			entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 25, 0, true, false, true));
		}
		if (
			entity.level() instanceof ServerLevel
				&& entity.tickCount % 4 == 0
				&& entity.isEyeInFluid(GranulesMod.HONEY_TAG)
				&& !entity.canBreatheUnderwater()
				&& !entity.hasEffect(MobEffects.WATER_BREATHING)
				&& (!(entity instanceof Player player) || !player.getAbilities().invulnerable)
		) {
			entity.setAirSupply(entity.getAirSupply() - 1);
		}
	}
}
