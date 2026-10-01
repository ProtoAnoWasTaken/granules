package com.protoano.granules.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.protoano.granules.client.HearthearthRenderStateAccess;
import com.protoano.granules.enchantment.HearthearthCampfire;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.CampfireRenderer;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireRenderer.class)
public abstract class HearthearthRendererMixin {
    @Shadow
    @Final
    private ItemModelResolver itemModelResolver;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/CampfireRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"))
    private void extractSwords(CampfireBlockEntity campfire, CampfireRenderState state, float partialTicks,
                               Vec3 camera, ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfo callback) {
        var swords = ((HearthearthRenderStateAccess) state).granules$plantedSwords();
        for (var sword : swords) {
            sword.clear();
        }
        for (var planted : HearthearthCampfire.get(campfire).swords()) {
            itemModelResolver.updateForTopItem(swords[planted.style()], planted.sword(), ItemDisplayContext.NONE,
                campfire.getLevel(), null, (int) campfire.getBlockPos().asLong() + planted.style());
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/CampfireRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void submitSwords(CampfireRenderState state, PoseStack poses, SubmitNodeCollector collector,
                              CameraRenderState camera, CallbackInfo callback) {
        var swords = ((HearthearthRenderStateAccess) state).granules$plantedSwords();
        for (int style = 0; style < swords.length; style++) {
            if (swords[style].isEmpty()) {
                continue;
            }
            poses.pushPose();
            poses.translate(0.5F, 0.7F, 0.5F);
            poses.mulPose(Axis.YP.rotationDegrees(style * 72.0F));
            if (style > 0) {
                poses.translate(0.13F, (style % 2) * 0.035F, 0.0F);
            }
            poses.mulPose(Axis.ZP.rotationDegrees(-135.0F + (style - 2) * 6.0F));
            poses.scale(0.95F, 0.95F, 0.95F);
            swords[style].submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }
}
