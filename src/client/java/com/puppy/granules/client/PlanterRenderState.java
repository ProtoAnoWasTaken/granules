package com.puppy.granules.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class PlanterRenderState extends BlockEntityRenderState {
	public final ItemStackRenderState crop = new ItemStackRenderState();
	public final MovingBlockRenderState cropBlock = new MovingBlockRenderState();
	public final MovingBlockRenderState fruitBlock = new MovingBlockRenderState();
	public boolean rendersCropBlock;
	public boolean rendersFruitBlock;
	public int growthAge;
	public int maximumGrowthAge;
}
