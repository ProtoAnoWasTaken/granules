package com.puppy.granules.client.mixin;

import com.puppy.granules.rabbit.PalePeltEquipment;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererOperaGloveMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void granules$animateOperaGlove(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo callbackInfo) {
        boolean glove = avatar.getOffhandItem().is(PalePeltEquipment.OPERA_GLOVE);
        if (!glove) {
            return;
        }
        HumanoidArm offArm = state.mainArm.getOpposite();
        if (offArm == HumanoidArm.LEFT) {
            state.leftHandItemState.clear();
        } else {
            state.rightHandItemState.clear();
        }
        boolean insulatedAction = PalePeltEquipment.insulatesMainHand(avatar);
        if (insulatedAction && state.attackTime > 0.0F) {
            state.attackArm = offArm;
        }
        if (insulatedAction && state.isUsingItem) {
            state.useItemHand = InteractionHand.OFF_HAND;
            if (offArm == HumanoidArm.LEFT) {
                state.leftArmPose = state.rightArmPose;
            } else {
                state.rightArmPose = state.leftArmPose;
            }
        }
    }
}
