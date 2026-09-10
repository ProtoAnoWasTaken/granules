package com.puppy.granules.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class PotionStackSizeMixin {
	public int getMaxStackSize() {
		ItemStack stack = (ItemStack) (Object) this;
		if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
			return 16;
		}

		return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
	}
}
