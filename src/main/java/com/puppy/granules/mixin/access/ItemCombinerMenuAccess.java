package com.puppy.granules.mixin.access;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccess {
	@Accessor("access")
	ContainerLevelAccess granules$getAccess();

	@Accessor("inputSlots")
	Container granules$getInputSlots();

	@Accessor("resultSlots")
	ResultContainer granules$getResultSlots();
}
