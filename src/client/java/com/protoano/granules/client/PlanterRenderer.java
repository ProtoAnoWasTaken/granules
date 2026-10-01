package com.protoano.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.protoano.granules.block.PlanterBlockEntity;
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
	private static final float MELON_SCALE = 0.35F;
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
        ItemStack displayed = planter.getRenderedCrop();
        state.fullCropModel = displayed.is(Items.GOLDEN_CARROT) || displayed.is(Items.GLISTERING_MELON_SLICE);
        state.melonStem = displayed.is(Items.GLISTERING_MELON_SLICE)
            || cropBlockState != null && (cropBlockState.is(net.minecraft.world.level.block.Blocks.MELON_STEM)
                || cropBlockState.is(net.minecraft.world.level.block.Blocks.ATTACHED_MELON_STEM));
        ItemStack fruit = ItemStack.EMPTY;
        if (displayed.is(Items.GLISTERING_MELON_SLICE) && planter.getGrowthAge() >= planter.getMaximumGrowthAge()) {
            fruit = displayed.copy();
            fruit.set(DataComponents.ITEM_MODEL, GLISTERING_MELON_MODEL);
        }
        itemModelResolver.updateForTopItem(state.customFruit, fruit, ItemDisplayContext.NONE, planter.getLevel(), null, seed + 2);
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
			itemModelResolver.updateForTopItem(state.crop, cropStack, state.fullCropModel ? ItemDisplayContext.NONE : ItemDisplayContext.FIXED, planter.getLevel(), null, seed + 1);
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
            return Identifier.fromNamespaceAndPath("granules", growthAge >= 7
                ? "planter_glistering_melon_attached" : "planter_glistering_melon_stem" + Math.clamp(growthAge, 0, 7));
		}
		return null;
	}

	@Override
	public void submit(PlanterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.rendersCropBlock) {
			poseStack.pushPose();
			poseStack.translate(0.1875F, 0.5625F, state.melonStem ? 0.3125F : 0.1875F);
			poseStack.scale(0.625F, 0.625F, 0.625F);
			submitNodeCollector.submitMovingBlock(poseStack, state.cropBlock, 0);
			poseStack.popPose();
		}
		if (state.rendersFruitBlock) {
			poseStack.pushPose();
			float fruitScale = state.melonStem ? MELON_SCALE : 0.4F;
			poseStack.translate(0.5F - fruitScale / 2, 0.58F, 0.18F - fruitScale / 2);
			poseStack.scale(fruitScale, fruitScale, fruitScale);
			submitNodeCollector.submitMovingBlock(poseStack, state.fruitBlock, 0);
			poseStack.popPose();
		}
		if (!state.rendersCropBlock && !state.crop.isEmpty()) {
			float progress = state.maximumGrowthAge == 0 ? 0.0F : (float) state.growthAge / state.maximumGrowthAge;
			float scale = state.fullCropModel ? 0.625F : 0.16F + progress * 0.34F;
			poseStack.pushPose();
			poseStack.translate(0.5F, state.fullCropModel ? 0.875F : 0.6F + progress * 0.15F, state.melonStem ? 0.625F : 0.5F);
            if (state.melonStem && state.growthAge >= state.maximumGrowthAge) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
            }
			poseStack.scale(scale, scale, scale);
			state.crop.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
        if (!state.customFruit.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.58F + MELON_SCALE / 2, 0.18F);
            poseStack.scale(MELON_SCALE, MELON_SCALE, MELON_SCALE);
            state.customFruit.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
	}
}
