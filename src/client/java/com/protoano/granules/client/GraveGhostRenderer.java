package com.protoano.granules.client;

import com.protoano.granules.grave.GraveContent;
import com.protoano.granules.grave.GraveGhostEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public final class GraveGhostRenderer extends EntityRenderer<ClientGraveGhost, GraveGhostRenderer.State> {
    private final AvatarRenderer<ClientGraveGhost> playerRenderer;
    private final AvatarRenderer<ClientGraveGhost> slimPlayerRenderer;

    public GraveGhostRenderer(EntityRendererProvider.Context context) {
        super(context);
        playerRenderer = new AvatarRenderer<>(context, false);
        slimPlayerRenderer = new AvatarRenderer<>(context, true);
        shadowRadius = 0;
    }

    public static void initialize() {
        EntityRendererRegistry.register(GraveContent.BURIAL, context -> new net.minecraft.client.renderer.entity.DisplayRenderer.ItemDisplayRenderer(context) {
        });
        GraveGhostEntity.clientFactory = ClientGraveGhost::new;
        EntityRendererRegistry.register(GraveContent.GHOST, context -> (EntityRenderer) new GraveGhostRenderer(context));
    }

    @Override
    public boolean shouldRender(ClientGraveGhost ghost, Frustum frustum, double x, double y, double z) {
        return ghost.visibleTo(Minecraft.getInstance().player) && super.shouldRender(ghost, frustum, x, y, z);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ClientGraveGhost ghost, State state, float partialTick) {
        super.extractRenderState(ghost, state, partialTick);
        var model = ghost.modelEntity();
        EntityRenderer renderer;
        if (model == ghost) {
            renderer = ghost.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM
                ? slimPlayerRenderer : playerRenderer;
        } else {
            renderer = entityRenderDispatcher.getRenderer(model);
        }
        state.model = renderer.createRenderState(model, partialTick);
        state.renderer = renderer;
        state.model.isInvisible = true;
        state.model.outlineColor = 0xFFB8D8FF;
        state.model.shadowPieces.clear();
        if (state.model instanceof LivingEntityRenderState living) {
            living.isInvisibleToPlayer = true;
        }
        state.outlineColor = state.model.outlineColor;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.model != null) {
            state.renderer.submit(state.model, pose, collector, camera);
        }
    }

    public static final class State extends EntityRenderState {
        public EntityRenderState model;
        public EntityRenderer renderer;
    }
}
