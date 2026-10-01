package com.protoano.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.protoano.granules.block.FenceLeashOrientation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;

public final class OrientedLeashKnotRenderer extends LeashKnotRenderer {
    public OrientedLeashKnotRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new KnotState();
    }

    @Override
    public void extractRenderState(LeashFenceKnotEntity entity, EntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ((KnotState) state).axis = FenceLeashOrientation.axis(entity);
    }

    public static void orient(PoseStack pose, Direction.Axis axis) {
        if (axis == Direction.Axis.Y) {
            return;
        }
        pose.translate(0.0, 0.125, 0.0);
        if (axis == Direction.Axis.X) {
            pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        pose.translate(0.0, -0.125, 0.0);
    }

    @Override
    public void submit(EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        orient(pose, ((KnotState) state).axis);
        super.submit(state, pose, collector, camera);
        pose.popPose();
    }

    public static final class KnotState extends EntityRenderState {
        public Direction.Axis axis = Direction.Axis.Y;
    }
}
