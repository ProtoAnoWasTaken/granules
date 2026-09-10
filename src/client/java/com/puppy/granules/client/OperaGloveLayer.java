package com.puppy.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.puppy.granules.rabbit.PalePeltEquipment;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class OperaGloveLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("granules", "opera_glove"),
        "main"
    );
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        "granules",
        "textures/entity/equipment/humanoid/pale_pelt_items.png"
    );
    private final HumanoidModel<AvatarRenderState> gloveModel;
    private static HumanoidModel<AvatarRenderState> firstPersonModel;

    public OperaGloveLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
        super(parent);
        this.gloveModel = new HumanoidModel<>(context.bakeLayer(MODEL_LAYER));
        firstPersonModel = this.gloveModel;
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.1F), 0.0F), 64, 32);
    }

    public static void submitFirstPersonArm(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        HumanoidArm arm
    ) {
        if (firstPersonModel == null) {
            return;
        }
        var modelPart = firstPersonModel.getArm(arm);
        modelPart.resetPose();
        modelPart.visible = true;
        modelPart.zRot = arm == HumanoidArm.RIGHT ? 0.1F : -0.1F;
        collector.submitModelPart(
            modelPart,
            poseStack,
            RenderTypes.entityTranslucent(TEXTURE),
            light,
            OverlayTexture.NO_OVERLAY,
            null
        );
    }

    @Override
    public void submit(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int light,
        AvatarRenderState state,
        float limbAngle,
        float limbDistance
    ) {
        ItemStack offhand = state.mainArm == HumanoidArm.RIGHT ? state.leftHandItemStack : state.rightHandItemStack;
        if (!offhand.is(PalePeltEquipment.OPERA_GLOVE)) {
            return;
        }
        this.gloveModel.setupAnim(state);
        this.gloveModel.head.visible = false;
        this.gloveModel.hat.visible = false;
        this.gloveModel.body.visible = false;
        this.gloveModel.rightArm.visible = state.mainArm == HumanoidArm.LEFT;
        this.gloveModel.leftArm.visible = state.mainArm == HumanoidArm.RIGHT;
        this.gloveModel.rightLeg.visible = false;
        this.gloveModel.leftLeg.visible = false;
        renderColoredCutoutModel(this.gloveModel, TEXTURE, poseStack, collector, light, state, -1, 0);
    }
}
