package com.protoano.granules.mixin;

import com.protoano.granules.sound.GranulesSounds;
import com.protoano.granules.item.EntityLocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandArmsMixin {
    @Unique
    private static final String GRANULES_STICK_ARMS = "granules:stick_arms";

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void granules$attachArms(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> callback) {
        ArmorStand stand = (ArmorStand) (Object) this;
        if (stand.showArms() || stand.isMarker() || EntityLocks.locked(stand) || player.isSpectator()
            || !player.getMainHandItem().is(Items.STICK) || !player.getOffhandItem().is(Items.STICK)) {
            return;
        }
        if (!player.getAbilities().mayBuild || !stand.level().mayInteract(player, stand.blockPosition())) {
            callback.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!stand.level().isClientSide()) {
            if (!stand.addTag(GRANULES_STICK_ARMS)) {
                callback.setReturnValue(InteractionResult.FAIL);
                return;
            }
            stand.setShowArms(true);
            player.getMainHandItem().consume(1, player);
            player.getOffhandItem().consume(1, player);
            stand.playSound(GranulesSounds.STAND_ARMS, 1.0F, 1.0F);
        }
        callback.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "brokenByAnything", at = @At("HEAD"))
    private void granules$returnArmSticks(ServerLevel level, DamageSource source, CallbackInfo callback) {
        ArmorStand stand = (ArmorStand) (Object) this;
        if (stand.entityTags().contains(GRANULES_STICK_ARMS)) {
            stand.removeTag(GRANULES_STICK_ARMS);
            Block.popResource(level, stand.blockPosition().above(), new ItemStack(Items.STICK, 2));
        }
    }
}
