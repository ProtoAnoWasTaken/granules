package com.puppy.granules.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityDataAccessor {
	@Accessor("entityData")
	SynchedEntityData granules$getEntityData();
}
