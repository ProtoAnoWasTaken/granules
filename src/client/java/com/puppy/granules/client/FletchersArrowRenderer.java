package com.puppy.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.puppy.granules.entity.FletchersArrowEntity;
import com.puppy.granules.fletching.ArrowParts;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

import java.util.List;

public class FletchersArrowRenderer extends EntityRenderer<FletchersArrowEntity, FletchersArrowRenderState> {
	private final ArrowModel model;

	public FletchersArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
	}

	@Override
	public FletchersArrowRenderState createRenderState() {
		return new FletchersArrowRenderState();
	}

	@Override
	public void extractRenderState(FletchersArrowEntity entity, FletchersArrowRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.parts = entity.parts();
		state.echoPhantom = entity.isEchoPhantom();
		state.xRot = entity.getXRot(partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.shake = entity.shakeTime - partialTicks;
		this.extractStringLeash(entity, state, partialTicks);
	}

	@Override
	public void submit(
		FletchersArrowRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		CameraRenderState camera
	) {
		poseStack.pushPose();
		poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(state.yRot - 90.0F));
		poseStack.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(state.xRot));
		if (state.echoPhantom) {
			this.submitPhantom(submitNodeCollector, state, poseStack, this.phantomTexture(state.parts));
		} else {
			this.submitLayer(submitNodeCollector, state, poseStack, this.shaftTexture(state.parts));
			this.submitLayer(submitNodeCollector, state, poseStack, this.fletchTexture(state.parts));
			this.submitLayer(submitNodeCollector, state, poseStack, this.headTexture(state.parts));
		}
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	private void submitLayer(
		SubmitNodeCollector submitNodeCollector,
		FletchersArrowRenderState state,
		PoseStack poseStack,
		Identifier texture
	) {
		submitNodeCollector.submitModel(
			(Model) this.model,
			state,
			poseStack,
			RenderTypes.entityTranslucent(texture),
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			state.outlineColor,
			null
		);
	}

	private void submitPhantom(
		SubmitNodeCollector submitNodeCollector,
		FletchersArrowRenderState state,
		PoseStack poseStack,
		Identifier texture
	) {
		submitNodeCollector.submitModel(
			(Model) this.model,
			state,
			poseStack,
			RenderTypes.entityTranslucent(texture),
			state.lightCoords,
			OverlayTexture.NO_OVERLAY,
			EntityRenderState.NO_OUTLINE,
			null
		);
	}

	private Identifier shaftTexture(ArrowParts parts) {
		return texture(parts.shaft().texture());
	}

	private Identifier fletchTexture(ArrowParts parts) {
		String name = switch (parts.fletch()) {
			case FEATHER -> "arrow_fletch_feather";
			case STRING -> "arrow_fletch_string";
			case FIREWORK -> "arrow_fletch_firework";
			case NAME_TAG -> "arrow_fletch_nametag";
		};
		return texture(name);
	}

	private Identifier headTexture(ArrowParts parts) {
		return texture(parts.head().entityTexture());
	}

	private Identifier phantomTexture(ArrowParts parts) {
		return texture("echo_fletchers_arrow_" + parts.identifierSuffix());
	}

	private Identifier texture(String filename) {
		return Identifier.fromNamespaceAndPath("granules", "textures/entity/projectiles/" + filename + ".png");
	}

	private void extractStringLeash(FletchersArrowEntity entity, FletchersArrowRenderState state, float partialTicks) {
		Entity owner = entity.getOwner();
		if (state.parts.fletch() != ArrowParts.Fletch.STRING || owner == null) {
			state.leashStates = null;
			return;
		}
		EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
		leash.offset = Vec3.ZERO;
		leash.start = entity.getPosition(partialTicks);
		net.minecraft.world.item.Item tetherWeapon = entity.getWeaponItem().is(net.minecraft.world.item.Items.CROSSBOW)
			? net.minecraft.world.item.Items.CROSSBOW
			: net.minecraft.world.item.Items.BOW;
		leash.end = owner.getPosition(partialTicks).add(owner.getHandHoldingItemAngle(tetherWeapon));
		leash.startBlockLight = 15;
		leash.endBlockLight = 15;
		leash.startSkyLight = 15;
		leash.endSkyLight = 15;
		leash.slack = false;
		state.leashStates = List.of(leash);
	}
}
