package com.puppy.granules.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.client.EnchantedEnchantTableRenderStateAccess;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantTableRenderer.class)
public class EnchantTableRendererMixin {
	@Unique
	private static final SpriteId GRANULES$ENCHANTED_BOOK_TEXTURE = Sheets.BLOCKS_MAPPER.apply(
		Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "enchanted_enchanting_table_book")
	);

	@Unique
	private static final ThreadLocal<Boolean> GRANULES$RENDERING_ENCHANTED_BOOK = ThreadLocal.withInitial(() -> false);

	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
		at = @At("TAIL")
	)
	private void granules$recordEnchantedTable(
		EnchantingTableBlockEntity table,
		EnchantTableRenderState state,
		float tickProgress,
		Vec3 cameraPosition,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
		CallbackInfo callbackInfo
	) {
		((EnchantedEnchantTableRenderStateAccess) state).granules$setEnchantedEnchantingTable(
			table.getBlockState().is(GranulesMod.ENCHANTED_ENCHANTING_TABLE)
		);
	}

	@Inject(
		method = "submit(Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("HEAD")
	)
	private void granules$beginBookSubmission(
		EnchantTableRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		CameraRenderState camera,
		CallbackInfo callbackInfo
	) {
		GRANULES$RENDERING_ENCHANTED_BOOK.set(
			((EnchantedEnchantTableRenderStateAccess) state).granules$isEnchantedEnchantingTable()
		);
	}

	@Redirect(
		method = "submit(Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/renderer/blockentity/EnchantTableRenderer;BOOK_TEXTURE:Lnet/minecraft/client/resources/model/sprite/SpriteId;"
		)
	)
	private SpriteId granules$selectBookTexture() {
		if (GRANULES$RENDERING_ENCHANTED_BOOK.get()) {
			return GRANULES$ENCHANTED_BOOK_TEXTURE;
		}
		return EnchantTableRenderer.BOOK_TEXTURE;
	}

	@Inject(
		method = "submit(Lnet/minecraft/client/renderer/blockentity/state/EnchantTableRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At("RETURN")
	)
	private void granules$finishBookSubmission(
		EnchantTableRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		CameraRenderState camera,
		CallbackInfo callbackInfo
	) {
		GRANULES$RENDERING_ENCHANTED_BOOK.remove();
	}
}
