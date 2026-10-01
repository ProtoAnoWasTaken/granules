package com.protoano.granules.mixin;

import com.protoano.granules.item.EntityLocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemFrame.class, ArmorStand.class})
public abstract class LockedDisplayMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void granules$keepItems(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> callback) {
        if (EntityLocks.locked((Entity) (Object) this)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void granules$protectDisplay(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (EntityLocks.locked((Entity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
