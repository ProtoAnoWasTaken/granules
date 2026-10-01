package com.protoano.granules.client;

import com.protoano.granules.chorus.ChorusContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.RaftModel;
import net.minecraft.client.renderer.entity.RaftRenderer;
import net.minecraft.resources.Identifier;

public final class ChorusClient {
    public static final ModelLayerLocation RAFT_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("granules", "boat/chorus"), "main");
    public static final ModelLayerLocation CHEST_RAFT_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("granules", "chest_boat/chorus"), "main");

    private ChorusClient() {
    }

    public static void initialize() {
        ModelLayerRegistry.registerModelLayer(RAFT_LAYER, RaftModel::createRaftModel);
        ModelLayerRegistry.registerModelLayer(CHEST_RAFT_LAYER, RaftModel::createChestRaftModel);
        EntityRendererRegistry.register(ChorusContent.RAFT_ENTITY, context -> new RaftRenderer(context, RAFT_LAYER));
        EntityRendererRegistry.register(ChorusContent.CHEST_RAFT_ENTITY, context -> new RaftRenderer(context, CHEST_RAFT_LAYER));
    }
}
