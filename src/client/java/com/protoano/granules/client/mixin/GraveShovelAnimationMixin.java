package com.protoano.granules.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.protoano.granules.grave.GraveContent;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class GraveShovelAnimationMixin {
    @Inject(method = "applyBrushTransform", at = @At("HEAD"), cancellable = true)
    private void lowerShovel(PoseStack pose, float partialTick, HumanoidArm arm, Player player, CallbackInfo callback) {
        if (!player.getUseItem().is(GraveContent.SHOVEL)) {
            return;
        }
        float elapsed = player.getTicksUsingItem() + partialTick;
        float dip = (float) (0.5 - 0.5 * Math.cos(elapsed * Math.PI / 5.0));
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(side * 0.1F, -0.55F * dip, -0.25F * dip);
        pose.mulPose(Axis.XP.rotationDegrees(-35F - 40F * dip));
        pose.mulPose(Axis.ZP.rotationDegrees(side * -15F));
        callback.cancel();
    }
}
