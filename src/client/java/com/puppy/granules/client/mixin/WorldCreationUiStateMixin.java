package com.puppy.granules.client.mixin;

import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.puppy.granules.client.GranulesClient;

@Mixin(WorldCreationUiState.class)
public class WorldCreationUiStateMixin {
	@Inject(method = "getPresetEditor", at = @At("RETURN"), cancellable = true)
	private void provideMultibiomeEditor(CallbackInfoReturnable<PresetEditor> callback) {
		WorldCreationUiState state = (WorldCreationUiState) (Object) this;
		if (state.getWorldType().preset().unwrapKey().filter(GranulesClient.MULTIBIOME_PRESET::equals).isPresent()) {
			callback.setReturnValue(GranulesClient.MULTIBIOME_EDITOR);
		}
	}
}
