package com.puppy.granules.mixin;

import com.puppy.granules.entity.FletchersArrowEntity;
import com.puppy.granules.fletching.ArrowParts;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMagicMixin {
	@Redirect(
		method = "onHitEntity",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
		)
	)
	private boolean applyFletchersArrowDamage(Entity target, DamageSource source, float damage) {
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		if (arrow instanceof FletchersArrowEntity fletchersArrow) {
			ArrowParts parts = fletchersArrow.parts();
			if (parts.fletch() == ArrowParts.Fletch.NAME_TAG) {
				if (parts.shaft() == ArrowParts.Shaft.END_ROD && target.is(EntityTypes.ENDERMAN)) {
					target.hurtOrSimulate(new DamageSource(arrow.damageSources().magic().typeHolder(), arrow, arrow), damage);
					return true;
				}
				return target.hurtOrSimulate(arrow.damageSources().arrow(arrow, arrow), damage);
			}
			if (parts.head() == ArrowParts.Head.GHAST || (
				parts.shaft() == ArrowParts.Shaft.END_ROD && target.is(EntityTypes.ENDERMAN)
			)) {
				Entity owner = arrow.getOwner();
				boolean isEndRodAgainstEnderman = parts.shaft() == ArrowParts.Shaft.END_ROD && target.is(EntityTypes.ENDERMAN);
				if (owner instanceof Player player && player.getAbilities().instabuild) {
					boolean wasHurt = target.hurtOrSimulate(arrow.damageSources().magic(), damage);
					return wasHurt || isEndRodAgainstEnderman;
				}
				if (owner != null) {
					boolean wasHurt = target.hurtOrSimulate(arrow.damageSources().indirectMagic(arrow, owner), damage);
					return wasHurt || isEndRodAgainstEnderman;
				}
				boolean wasHurt = target.hurtOrSimulate(arrow.damageSources().magic(), damage);
				return wasHurt || isEndRodAgainstEnderman;
			}
		}
		return target.hurtOrSimulate(source, damage);
	}
}
