package com.puppy.granules.mixin;

import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Rabbit.class)
public interface RabbitAccessor {
    @Invoker("setVariant")
    void granules$setVariant(Rabbit.Variant variant);
}
