package com.puppy.granules.client;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class MoverRenderState extends EntityRenderState {
	public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}
