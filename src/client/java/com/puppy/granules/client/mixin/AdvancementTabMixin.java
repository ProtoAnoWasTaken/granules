package com.puppy.granules.client.mixin;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
	@Shadow
	public abstract AdvancementNode getRootNode();

	@Inject(method = "getTitle", at = @At("HEAD"), cancellable = true)
	private void granules$burrowTitle(CallbackInfoReturnable<Component> callback) {
		if (getRootNode().holder().id().equals(Identifier.fromNamespaceAndPath("granules", "call_me_paquerette"))) {
			callback.setReturnValue(Component.translatable("dimension.granules.the_burrow"));
		}
	}
}
