package com.puppy.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.puppy.granules.entity.MoverEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class MoverRenderer extends EntityRenderer<MoverEntity, MoverRenderState> {
	public MoverRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.5F;
	}

	@Override
	public boolean shouldRender(MoverEntity entity, Frustum culler, double camX, double camY, double camZ) {
		return super.shouldRender(entity, culler, camX, camY, camZ);
	}

	@Override
	public MoverRenderState createRenderState() {
		return new MoverRenderState();
	}

	@Override
	public void extractRenderState(MoverEntity entity, MoverRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
		state.movingBlockRenderState.randomSeedPos = entity.getStartPos();
		state.movingBlockRenderState.blockPos = pos;
		state.movingBlockRenderState.blockState = entity.getBlockState();
		if (entity.level() instanceof ClientLevel clientLevel) {
			state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
			state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
			state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
		}
	}

	@Override
	public void submit(MoverRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		BlockState blockState = state.movingBlockRenderState.blockState;
		if (blockState.getRenderShape() != RenderShape.MODEL) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(-0.5D, 0.0D, -0.5D);
		submitNodeCollector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}
}
