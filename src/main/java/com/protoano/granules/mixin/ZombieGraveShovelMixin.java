package com.protoano.granules.mixin;

import com.protoano.granules.grave.GraveLoot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieGraveShovelMixin {
    @Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
    private void equipGraveShovel(RandomSource random, DifficultyInstance difficulty, CallbackInfo callback) {
        GraveLoot.equipZombie((Zombie) (Object) this, random);
    }
}
