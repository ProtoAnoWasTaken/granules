package com.puppy.granules.client;

import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class OldWorldCodRenderer extends CodRenderer {
	private static final Identifier OLD_WORLD_COD_TEXTURE = Identifier.fromNamespaceAndPath(
		"granules",
		"textures/entity/ow_cod.png"
	);

	public OldWorldCodRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return OLD_WORLD_COD_TEXTURE;
	}
}
