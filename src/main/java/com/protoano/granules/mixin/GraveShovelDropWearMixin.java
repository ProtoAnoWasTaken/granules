package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.protoano.granules.grave.GraveContent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class GraveShovelDropWearMixin {
    @WrapOperation(method = "dropCustomDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"))
    private void retainShovelWear(ItemStack stack, int damage, Operation<Void> original) {
        if (stack.is(GraveContent.SHOVEL)) {
            damage = Math.max(damage, stack.getDamageValue());
        }
        original.call(stack, damage);
    }
}
