package com.puppy.granules.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.puppy.granules.rabbit.PalePeltEquipment;
import com.puppy.granules.client.OperaGloveLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererOperaGloveMixin {
    @Unique
    private boolean granules$renderingOperaGloveArm;

    @Unique
    private boolean granules$suppressingMainHandAction;

    @WrapOperation(
        method = "submitHandsWithItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            ordinal = 0
        )
    )
    private void granules$renderMainItemFromGlovedHand(
        ItemInHandRenderer renderer,
        AbstractClientPlayer player,
        float partialTick,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Operation<Void> original
    ) {
        if (!PalePeltEquipment.insulatesMainHand(player)) {
            original.call(
                renderer,
                player,
                partialTick,
                pitch,
                hand,
                swingProgress,
                stack,
                equippedProgress,
                poseStack,
                collector,
                light
            );
            return;
        }
        granules$suppressingMainHandAction = true;
        try {
            original.call(
                renderer,
                player,
                partialTick,
                pitch,
                hand,
                0.0F,
                stack,
                equippedProgress,
                poseStack,
                collector,
                light
            );
        } finally {
            granules$suppressingMainHandAction = false;
        }
    }

    @WrapOperation(
        method = "submitHandsWithItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            ordinal = 1
        )
    )
    private void granules$hideOperaGlove(
        ItemInHandRenderer renderer,
        AbstractClientPlayer player,
        float partialTick,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Operation<Void> original
    ) {
        if (!stack.is(PalePeltEquipment.OPERA_GLOVE)) {
            original.call(
                renderer,
                player,
                partialTick,
                pitch,
                hand,
                swingProgress,
                stack,
                equippedProgress,
                poseStack,
                collector,
                light
            );
            return;
        }
        if (!PalePeltEquipment.insulatesMainHand(player)) {
            return;
        }
        float actionProgress = player.getAttackAnim(partialTick);
        if (!player.isUsingItem() && actionProgress <= 0.0F) {
            return;
        }
        granules$renderingOperaGloveArm = true;
        try {
            original.call(
                renderer,
                player,
                partialTick,
                pitch,
                InteractionHand.MAIN_HAND,
                actionProgress,
                ItemStack.EMPTY,
                equippedProgress,
                poseStack,
                collector,
                light
            );
        } finally {
            granules$renderingOperaGloveArm = false;
        }
    }

    @Redirect(
        method = "submitArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getMainArm()Lnet/minecraft/world/entity/HumanoidArm;"
        )
    )
    private HumanoidArm granules$selectOperaGloveArm(AbstractClientPlayer player) {
        HumanoidArm mainArm = player.getMainArm();
        if (granules$renderingOperaGloveArm) {
            return mainArm.getOpposite();
        }
        return mainArm;
    }

    @Redirect(
        method = "submitArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;"
        )
    )
    private InteractionHand granules$selectAnimatedHand(AbstractClientPlayer player) {
        if (granules$suppressingMainHandAction || granules$renderingOperaGloveArm) {
            return InteractionHand.OFF_HAND;
        }
        return player.getUsedItemHand();
    }

    @WrapOperation(
        method = "renderPlayerArm",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
        )
    )
    private void granules$renderRightOperaGlove(
        AvatarRenderer<?> renderer,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Identifier texture,
        boolean sleeve,
        Operation<Void> original
    ) {
        if (granules$renderingOperaGloveArm) {
            OperaGloveLayer.submitFirstPersonArm(poseStack, collector, light, HumanoidArm.RIGHT);
            return;
        }
        original.call(renderer, poseStack, collector, light, texture, sleeve);
    }

    @WrapOperation(
        method = "renderPlayerArm",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"
        )
    )
    private void granules$renderLeftOperaGlove(
        AvatarRenderer<?> renderer,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        Identifier texture,
        boolean sleeve,
        Operation<Void> original
    ) {
        if (granules$renderingOperaGloveArm) {
            OperaGloveLayer.submitFirstPersonArm(poseStack, collector, light, HumanoidArm.LEFT);
            return;
        }
        original.call(renderer, poseStack, collector, light, texture, sleeve);
    }
}
