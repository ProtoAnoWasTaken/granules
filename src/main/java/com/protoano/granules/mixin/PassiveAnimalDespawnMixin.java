package com.protoano.granules.mixin;

import com.protoano.granules.GranulesMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class PassiveAnimalDespawnMixin {
    @Shadow
    private EntityReference<ServerPlayer> loveCause;

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void granules$preservePreviouslyFedAnimals(ValueInput input, CallbackInfo callback) {
        if (this.loveCause != null) {
            ((Animal)(Object)this).setPersistenceRequired();
        }
    }

    @Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void granules$allowWildAnimalDespawn(double distanceSquared, CallbackInfoReturnable<Boolean> callback) {
        Animal animal = (Animal)(Object)this;
        if (animal.getType().getCategory() == MobCategory.CREATURE
            && animal.level() instanceof ServerLevel level
            && level.getGameRules().get(GranulesMod.LEGACY_PASSIVE_MOBS)) {
            callback.setReturnValue(true);
        }
    }
}
