package com.puppy.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.puppy.granules.block.PlanterBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlanterRenderer implements BlockEntityRenderer<PlanterBlockEntity, PlanterRenderState> {
	private static final Identifier GOLDEN_CARROT_STAGE_0_MODEL = Identifier.fromNamespaceAndPath("granules", "planter_golden_carrot_stage0");
	private static final Identifier GOLDEN_CARROT_STAGE_1_MODEL = Identifier.fromNamespaceAndPath("granules", "planter_golden_carrot_stage1");
	private static final Identifier GOLDEN_CARROT_STAGE_2_MODEL = Identifier.fromNamespaceAndPath("granules", "planter_golden_carrot_stage2");
	private static final Identifier GOLDEN_CARROT_STAGE_3_MODEL = Identifier.fromNamespaceAndPath("granules", "planter_golden_carrot_stage3");
	private static final Identifier GLISTERING_MELON_MODEL = Identifier.fromNamespaceAndPath("granules", "planter_glistering_melon");
	private final ItemModelResolver itemModelResolver;

	public PlanterRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public PlanterRenderState createRenderState() {
		return new PlanterRenderState();
	}

	@Override
	public void extractRenderState(
		PlanterBlockEntity planter,
		PlanterRenderState state,
		float partialTicks,
		Vec3 cameraPosition,
		ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
	) {
		BlockEntityRenderer.super.extractRenderState(planter, state, partialTicks, cameraPosition, breakProgress);
		int seed = (int) planter.getBlockPos().asLong();
		BlockState cropBlockState = planter.getRenderedCropState();
		state.rendersCropBlock = cropBlockState != null && cropBlockState.getRenderShape() == RenderShape.MODEL;
		if (state.rendersCropBlock) {
			populateCropBlockState(planter, state.cropBlock, cropBlockState);
			itemModelResolver.updateForTopItem(state.crop, ItemStack.EMPTY, ItemDisplayContext.FIXED, planter.getLevel(), null, seed + 1);
		} else {
			ItemStack cropStack = planter.getRenderedCrop().copy();
			Identifier cropModel = cropModelFor(cropStack, planter.getGrowthAge());
			if (cropModel != null) {
				cropStack.set(DataComponents.ITEM_MODEL, cropModel);
			}
			itemModelResolver.updateForTopItem(state.crop, cropStack, ItemDisplayContext.FIXED, planter.getLevel(), null, seed + 1);
		}
		BlockState fruitBlockState = planter.getRenderedFruitState();
		state.rendersFruitBlock = fruitBlockState != null && fruitBlockState.getRenderShape() == RenderShape.MODEL;
		if (state.rendersFruitBlock) {
			populateCropBlockState(planter, state.fruitBlock, fruitBlockState);
		}
		state.growthAge = planter.getGrowthAge();
		state.maximumGrowthAge = planter.getMaximumGrowthAge();
	}

	private static void populateCropBlockState(PlanterBlockEntity planter, MovingBlockRenderState cropBlock, BlockState cropBlockState) {
		cropBlock.randomSeedPos = planter.getBlockPos();
		cropBlock.blockPos = planter.getBlockPos();
		cropBlock.blockState = cropBlockState;
		if (planter.getLevel() instanceof ClientLevel clientLevel) {
			cropBlock.biome = clientLevel.getBiome(planter.getBlockPos());
			cropBlock.cardinalLighting = clientLevel.cardinalLighting();
			cropBlock.lightEngine = clientLevel.getLightEngine();
		}
	}

	private static Identifier cropModelFor(ItemStack cropStack, int growthAge) {
		if (cropStack.is(Items.GOLDEN_CARROT)) {
			return switch (Math.min(3, growthAge)) {
				case 0 -> GOLDEN_CARROT_STAGE_0_MODEL;
				case 1 -> GOLDEN_CARROT_STAGE_1_MODEL;
				case 2 -> GOLDEN_CARROT_STAGE_2_MODEL;
				default -> GOLDEN_CARROT_STAGE_3_MODEL;
			};
		}
		if (cropStack.is(Items.GLISTERING_MELON_SLICE)) {
			return GLISTERING_MELON_MODEL;
		}
		return null;
	}

	@Override
	public void submit(PlanterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.rendersCropBlock) {
			poseStack.pushPose();
			poseStack.translate(0.1875F, 0.5625F, 0.1875F);
			poseStack.scale(0.625F, 0.625F, 0.625F);
			submitNodeCollector.submitMovingBlock(poseStack, state.cropBlock, 0);
			poseStack.popPose();
		}
		if (state.rendersFruitBlock) {
			poseStack.pushPose();
			poseStack.translate(0.3F, 0.58F, -0.02F);
			poseStack.scale(0.4F, 0.4F, 0.4F);
			submitNodeCollector.submitMovingBlock(poseStack, state.fruitBlock, 0);
			poseStack.popPose();
		}
		if (!state.rendersCropBlock && !state.crop.isEmpty()) {
			float progress = state.maximumGrowthAge == 0 ? 0.0F : (float) state.growthAge / state.maximumGrowthAge;
			float scale = 0.16F + progress * 0.34F;
			poseStack.pushPose();
			poseStack.translate(0.5F, 0.6F + progress * 0.15F, 0.5F);
			poseStack.scale(scale, scale, scale);
			state.crop.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}
}
