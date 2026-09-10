package com.puppy.granules.mixin;

import com.puppy.granules.block.PlanterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractThrownPotion.class)
public abstract class ThrownWaterPotionPlanterMixin {
	@Inject(method = "onHit", at = @At("HEAD"))
	private void granules$moisturizePlanters(HitResult hitResult, CallbackInfo callbackInfo) {
		AbstractThrownPotion potion = (AbstractThrownPotion) (Object) this;
		if (!(potion.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		PotionContents contents = potion.getItem().get(DataComponents.POTION_CONTENTS);
		if (contents == null || !contents.is(Potions.WATER)) {
			return;
		}
		BlockPos center = BlockPos.containing(hitResult.getLocation());
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
			if (pos.distSqr(center) > 16.0D) {
				continue;
			}
			if (serverLevel.getBlockEntity(pos) instanceof PlanterBlockEntity planter) {
				planter.moisturizeFromThrownWater();
			}
		}
	}
}
