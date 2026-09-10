package com.puppy.granules.client.mixin;

import com.puppy.granules.client.PurgeDimensionsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditWorldScreen.class)
public abstract class EditWorldScreenMixin {
	@Shadow
	private LinearLayout layout;

	@Shadow
	private LevelStorageSource.LevelStorageAccess levelAccess;

	@Inject(
		method = "<init>",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/gui/screens/worldselection/EditWorldScreen;OPTIMIZE_BUTTON:Lnet/minecraft/network/chat/Component;",
			shift = At.Shift.BEFORE
		)
	)
	private void granules$addPurgeDimensionsButton(
		Minecraft minecraft,
		LevelStorageSource.LevelStorageAccess levelAccess,
		String levelName,
		it.unimi.dsi.fastutil.booleans.BooleanConsumer callback,
		CallbackInfo ci
	) {
		EditWorldScreen screen = (EditWorldScreen) (Object) this;
		this.layout.addChild(Button.builder(
			Component.translatable("granules.purge_dimensions.button"),
			button -> minecraft.gui.setScreen(new PurgeDimensionsScreen(screen, this.levelAccess))
		).width(200).build());
	}
}
