package com.protoano.granules.mixin;

import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Phantom.class)
public interface PhantomFlightAccessor {
    @Accessor("moveTargetPoint")
    void granules$setFlightTarget(Vec3 target);
}
