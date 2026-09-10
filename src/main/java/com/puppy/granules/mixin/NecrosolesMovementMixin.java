package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class NecrosolesMovementMixin {
    @Inject(method = "isSwimming", at = @At("RETURN"), cancellable = true)
    private void preventSwimming(CallbackInfoReturnable<Boolean> callback) {
        Player self = (Player) (Object) this;
        if (GranulesEnchantments.level(self.getItemBySlot(EquipmentSlot.FEET), GranulesEnchantments.NECROSOLES) > 0) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void controlUnderwaterMovement(CallbackInfo callback) {
        Player self = (Player) (Object) this;
        if (!self.isInWater() || GranulesEnchantments.level(self.getItemBySlot(EquipmentSlot.FEET), GranulesEnchantments.NECROSOLES) == 0) {
            return;
        }
        self.setSwimming(false);
        Vec3 movement = self.getDeltaMovement();
        double verticalMovement;
        if (self.isJumping()) {
            verticalMovement = 0.28D;
        } else if (self.isShiftKeyDown()) {
            verticalMovement = -0.18D;
        } else if (self.onGround()) {
            verticalMovement = movement.y;
        } else {
            verticalMovement = -0.10D;
        }
        self.setDeltaMovement(movement.x, verticalMovement, movement.z);
    }
}
