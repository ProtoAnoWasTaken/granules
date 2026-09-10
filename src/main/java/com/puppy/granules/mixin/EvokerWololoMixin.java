package com.puppy.granules.mixin;

import com.puppy.granules.world.EvokerConvertedSheepAccess;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.monster.illager.Evoker$EvokerWololoSpellGoal")
public abstract class EvokerWololoMixin {
	@Redirect(
		method = "performSpellCasting",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;setColor(Lnet/minecraft/world/item/DyeColor;)V"
		)
	)
	private void granules$makeWololoSheepAggressive(Sheep sheep, DyeColor color) {
		sheep.setColor(color);
		if (color == DyeColor.RED) {
			((EvokerConvertedSheepAccess) sheep).granules$setEvokerConverted(true);
		}
	}
}
