package com.puppy.granules.mixin.access;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccess {
	@Invoker("addDataSlot")
	DataSlot granules$addDataSlot(DataSlot dataSlot);
}
