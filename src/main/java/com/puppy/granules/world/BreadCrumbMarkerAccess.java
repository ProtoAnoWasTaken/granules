package com.puppy.granules.world;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface BreadCrumbMarkerAccess {
	@Nullable
	GlobalPos granules$getBreadCrumbMarker();

	void granules$setBreadCrumbMarker(@Nullable GlobalPos marker);

	void granules$beginBreadCrumbEating(InteractionHand hand, ItemStack stack);
}
