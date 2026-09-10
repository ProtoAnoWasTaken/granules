package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedWorkstationAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneResultSlotMixin {
	@Shadow
	@Final
	private ContainerLevelAccess val$access;

	@Inject(method = "onTake", at = @At("TAIL"))
	private void granules$grantStrength(Player player, ItemStack stack, CallbackInfo callbackInfo) {
		if (EnchantedWorkstationAccess.isAt(val$access, GranulesMod.ENCHANTED_GRINDSTONE)) {
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2400, 0));
		}
	}

	@Redirect(
		method = "lambda$onTake$0",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V")
	)
	private void granules$awardExtraExperience(ServerLevel level, Vec3 pos, int amount) {
		BlockPos blockPos = BlockPos.containing(pos);
		if (level.getBlockState(blockPos).is(GranulesMod.ENCHANTED_GRINDSTONE)) {
			amount = Math.round(amount * 1.5F);
		}
		ExperienceOrb.award(level, pos, amount);
	}
}
