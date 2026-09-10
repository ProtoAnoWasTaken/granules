package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import java.util.Set;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityHoneyMixin {
	@ModifyArg(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityFluidInteraction;<init>(Ljava/util/Set;)V"
		),
		index = 0
	)
	private Set<TagKey<Fluid>> granules$trackHoneyFluid(Set<TagKey<Fluid>> trackedFluids) {
		return Set.of(FluidTags.WATER, FluidTags.LAVA, GranulesMod.HONEY_TAG);
	}

	@Redirect(
		method = "baseTick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z")
	)
	private boolean granules$recognizeHoneyWhileSubmerged(Entity entity, TagKey<Fluid> fluidTag) {
		return entity.isEyeInFluid(fluidTag) || (fluidTag == FluidTags.WATER && entity.isEyeInFluid(GranulesMod.HONEY_TAG));
	}

}
