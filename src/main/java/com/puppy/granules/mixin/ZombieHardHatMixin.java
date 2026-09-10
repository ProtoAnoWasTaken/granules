package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieHardHatMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void granules$equipCaveHardHat(RandomSource random, DifficultyInstance difficulty, CallbackInfo callbackInfo) {
		Zombie zombie = (Zombie) (Object) this;
		if (zombie.level().canSeeSky(zombie.blockPosition()) || zombie.blockPosition().getY() >= zombie.level().getSeaLevel()) {
			return;
		}
		boolean rolledArmor = !zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
			|| !zombie.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
			|| !zombie.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
			|| !zombie.getItemBySlot(EquipmentSlot.FEET).isEmpty();
		if (!rolledArmor || random.nextInt(3) != 0) {
			return;
		}
		ItemStack hardHat = new ItemStack(GranulesMod.HARD_HAT);
		hardHat.setDamageValue(random.nextInt(hardHat.getMaxDamage()));
		zombie.setItemSlot(EquipmentSlot.HEAD, hardHat);
	}
}
