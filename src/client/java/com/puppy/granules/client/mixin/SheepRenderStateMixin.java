package com.puppy.granules.client.mixin;

import com.puppy.granules.client.GlowSheepRenderStateAccess;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SheepRenderState.class)
public class SheepRenderStateMixin implements GlowSheepRenderStateAccess {
	@Unique
	private boolean granules$hasGlowWool;

	@Override
	public boolean granules$hasGlowWool() {
		return granules$hasGlowWool;
	}

	@Override
	public void granules$setGlowWool(boolean hasGlowWool) {
		granules$hasGlowWool = hasGlowWool;
	}
}
