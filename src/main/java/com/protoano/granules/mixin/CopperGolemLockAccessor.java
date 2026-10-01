package com.protoano.granules.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CopperGolem.class)
public interface CopperGolemLockAccessor {
    @Invoker("turnToStatue")
    void granules$turnToStatue(ServerLevel level);
}
