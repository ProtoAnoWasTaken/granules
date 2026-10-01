package com.protoano.granules.client;

import com.protoano.granules.aspen.AspenContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.Identifier;

public final class AspenClient {
    public static final ModelLayerLocation BOAT_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("granules", "boat/aspen"), "main");
    public static final ModelLayerLocation CHEST_BOAT_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("granules", "chest_boat/aspen"), "main");

    private AspenClient() {
    }

    public static void initialize() {
        ModelLayerRegistry.registerModelLayer(BOAT_LAYER, BoatModel::createBoatModel);
        ModelLayerRegistry.registerModelLayer(CHEST_BOAT_LAYER, BoatModel::createChestBoatModel);
        EntityRendererRegistry.register(AspenContent.BOAT_ENTITY, context -> new BoatRenderer(context, BOAT_LAYER));
        EntityRendererRegistry.register(AspenContent.CHEST_BOAT_ENTITY, context -> new BoatRenderer(context, CHEST_BOAT_LAYER));
    }
}
