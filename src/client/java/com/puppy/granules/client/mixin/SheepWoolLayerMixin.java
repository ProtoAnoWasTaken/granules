package com.puppy.granules.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.puppy.granules.client.GlowSheepRenderStateAccess;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheepWoolLayer.class)
public abstract class SheepWoolLayerMixin {
	@Unique
	private SheepRenderState granules$renderState;

	@Inject(method = "submit", at = @At("HEAD"))
	private void granules$rememberRenderState(
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int packedLight,
		SheepRenderState renderState,
		float limbSwing,
		float limbSwingAmount,
		CallbackInfo callbackInfo
	) {
		granules$renderState = renderState;
	}

	@ModifyArg(
		method = "submit",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/layers/SheepWoolLayer;coloredCutoutModelCopyLayerRender(Lnet/minecraft/client/model/Model;Lnet/minecraft/resources/Identifier;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;II)V"
		),
		index = 4
	)
	private int granules$makeGlowWoolEmissive(int packedLight) {
		if (granules$renderState != null && ((GlowSheepRenderStateAccess) granules$renderState).granules$hasGlowWool()) {
			return 15728880;
		}
		return packedLight;
	}
}
