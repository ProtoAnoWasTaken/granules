package com.puppy.granules.client;

import com.puppy.granules.fletching.ArrowParts;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;

public class FletchersArrowRenderState extends ArrowRenderState {
	public ArrowParts parts = new ArrowParts(ArrowParts.Shaft.STICK, ArrowParts.Fletch.FEATHER, ArrowParts.Head.FLINT);
	public boolean echoPhantom;
}
