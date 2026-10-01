package com.protoano.granules.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.protoano.granules.client.title.TitleRenderState;
import com.protoano.granules.title.PlayerTitles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class PlayerTitleRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void granules$extractTitle(Entity entity, EntityRenderState state, float partialTick, CallbackInfo callback) {
        if (state instanceof TitleRenderState titleState) {
            var title = entity instanceof Player player ? player.getAttached(PlayerTitles.TITLE) : null;
            titleState.granules$title(title == null ? null : title.displayName());
        }
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V", at = @At("TAIL"))
    private void granules$renderTitle(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, int offset, CallbackInfo callback) {
        if (state.nameTag == null || state.nameTagAttachment == null || !(state instanceof TitleRenderState titleState)
            || titleState.granules$title() == null) {
            return;
        }
        var font = Minecraft.getInstance().font;
        Component title = titleState.granules$title();
        int usernameWidth = font.width(state.nameTag) + 2;
        int padding = Math.max(0, (int) Math.ceil((usernameWidth / 0.45F - font.width(title) - 2) / (2 * Math.max(1, font.width(" ")))));
        if (padding > 0) {
            String spaces = " ".repeat(padding);
            title = Component.literal(spaces).append(title).append(spaces);
        }
        float scale = (float) usernameWidth / (font.width(title) + 2);
        pose.pushPose();
        Vec3 anchor = state.nameTagAttachment;
        pose.translate(anchor.x, anchor.y + 0.24 - 0.5 * scale, anchor.z);
        pose.scale(scale, scale, scale);
        collector.submitNameTag(pose, Vec3.ZERO, offset, title, !state.isDiscrete, state.lightCoords, camera);
        pose.popPose();
    }
}
