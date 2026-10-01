package com.protoano.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.protoano.granules.block.PetBedBlock;
import com.protoano.granules.pet.DynamicPetBeds;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public final class DynamicPetBedRenderer implements BlockEntityRenderer<DynamicPetBeds.BedEntity, DynamicPetBedRenderer.State> {
    public DynamicPetBedRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static final class State extends BlockEntityRenderState {
        private DynamicPetBedVisuals.Parts parts;
        private float rotation;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DynamicPetBeds.BedEntity bed, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(bed, state, partialTicks, camera, breaking);
        state.parts = DynamicPetBedVisuals.extract(bed.materials());
        state.rotation = switch (bed.getBlockState().getValue(PetBedBlock.FACING)) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
        pose.translate(-0.5, 0, -0.5);
        DynamicPetBedVisuals.render(state.parts, pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
