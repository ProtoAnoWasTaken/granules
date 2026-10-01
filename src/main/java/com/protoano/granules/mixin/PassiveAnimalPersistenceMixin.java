package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class PassiveAnimalPersistenceMixin {
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void granules$protectKeptAnimals(CallbackInfo callback) {
        Mob mob = (Mob)(Object)this;
        if (mob instanceof Animal
            && (mob.hasCustomName()
                || mob.isPersistenceRequired()
                || mob.requiresCustomPersistence()
                || mob instanceof TamableAnimal pet && pet.isTame()
                || mob instanceof AbstractHorse horse && horse.isTamed())) {
            mob.setNoActionTime(0);
            callback.cancel();
        }
    }

    @Inject(method = "usePlayerItem", at = @At("HEAD"))
    private void granules$rememberFeeding(Player player, InteractionHand hand, ItemStack stack, CallbackInfo callback) {
        if ((Object)this instanceof Animal animal && !animal.level().isClientSide() && animal.isFood(stack)) {
            animal.setPersistenceRequired();
        }
    }

    @WrapOperation(
        method = "interact",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;")
    )
    private InteractionResult granules$rememberSpecialFeeding(Mob mob, Player player, InteractionHand hand, Operation<InteractionResult> original) {
        boolean offeredFood = mob instanceof Animal animal && animal.isFood(player.getItemInHand(hand));
        InteractionResult result = original.call(mob, player, hand);
        if (offeredFood && result.consumesAction() && !mob.level().isClientSide()) {
            mob.setPersistenceRequired();
        }
        return result;
    }
}
