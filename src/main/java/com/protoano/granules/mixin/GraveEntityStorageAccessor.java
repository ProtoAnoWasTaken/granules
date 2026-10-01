package com.protoano.granules.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentEntitySectionManager.class)
public interface GraveEntityStorageAccessor {
    @Accessor("permanentStorage")
    EntityPersistentStorage<Entity> granules$storage();
}
