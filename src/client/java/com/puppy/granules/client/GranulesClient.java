package com.puppy.granules.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.CheckerboardColumnBiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.bomb.BombContent;
import com.puppy.granules.network.VoidFogPayload;

import java.util.List;

public class GranulesClient implements ClientModInitializer {
	private static boolean enchantedCauldronTintRegistered;

	public static final ResourceKey<WorldPreset> MULTIBIOME_PRESET = ResourceKey.create(
		Registries.WORLD_PRESET,
		Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "multibiome")
	);
	public static final PresetEditor MULTIBIOME_EDITOR = GranulesClient::createMultibiomeScreen;

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(OperaGloveLayer.MODEL_LAYER, OperaGloveLayer::createLayer);
		UnmarkedDiscTint.initialize();
		RangeSelectItemModelProperties.ID_MAPPER.put(
			Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "ender_radar_pulse"),
			EnderRadarPulseProperty.MAP_CODEC
		);
		ChunkLoadPerformanceController.initialize(GranulesConfig.load().chunkLoadPriority());
		BoatJumpClientController.initialize();
		CasterDebugBeamRenderer.initialize();
		MoverRoutePreviewRenderer.initialize();
		FluidModel.Unbaked honeyFluidModel = new FluidModel.Unbaked(
			new Material(Identifier.withDefaultNamespace("block/honey_block_top")),
			new Material(Identifier.withDefaultNamespace("block/honey_block_top")),
			null,
			null
		);
		FluidRenderingRegistry.register(GranulesMod.HONEY, GranulesMod.FLOWING_HONEY, honeyFluidModel);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!enchantedCauldronTintRegistered && client.getBlockColors() != null) {
				client.getBlockColors().register(
					List.of(new EnchantedCauldronTintSource()),
					GranulesMod.ENCHANTED_CAULDRON
				);
				enchantedCauldronTintRegistered = true;
			}
			VoidFogClient.tick();
			MoverSoundController.tick(client);
		});
		ClientPlayNetworking.registerGlobalReceiver(VoidFogPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> VoidFogClient.setEnabled(payload.enabled()));
		});
		MenuScreens.register(GranulesMod.FLETCHING_MENU, FletchingScreen::new);
		MenuScreens.register(GranulesMod.PET_BED_MENU, PetBedScreen::new);
		EntityRendererRegistry.register(GranulesMod.FLETCHERS_ARROW_ENTITY, FletchersArrowRenderer::new);
		EntityRendererRegistry.register(GranulesMod.MOVER_ENTITY, MoverRenderer::new);
		EntityRendererRegistry.register(GranulesMod.OLD_WORLD_COD_ENTITY, OldWorldCodRenderer::new);
		EntityRendererRegistry.register(BombContent.BOMB_ENTITY, net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		BlockEntityRendererRegistry.register(GranulesMod.PLANTER_BLOCK_ENTITY, PlanterRenderer::new);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatarRenderer) {
				@SuppressWarnings("unchecked")
				var parent = (net.minecraft.client.renderer.entity.RenderLayerParent<net.minecraft.client.renderer.entity.state.AvatarRenderState, net.minecraft.client.model.player.PlayerModel>) avatarRenderer;
				helper.register(new OperaGloveLayer(parent, context));
			}
		});
	}

	private static Screen createMultibiomeScreen(CreateWorldScreen parent, WorldCreationContext context) {
		return new MultibiomeSelectionScreen(parent, context);
	}

	public static void applySelectedBiomes(CreateWorldScreen parent, List<Holder<Biome>> selectedBiomes) {
		parent.getUiState().updateDimensions((registries, dimensions) -> {
			if (selectedBiomes.isEmpty()) {
				return dimensions;
			}

			ChunkGenerator overworldGenerator = dimensions.overworld();
			if (!(overworldGenerator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
				return dimensions;
			}

			BiomeSource biomeSource = selectedBiomes.size() == 1
				? new FixedBiomeSource(selectedBiomes.getFirst())
				: new CheckerboardColumnBiomeSource(HolderSet.direct(selectedBiomes), 6);
			NoiseBasedChunkGenerator multibiomeGenerator = new NoiseBasedChunkGenerator(
				biomeSource,
				noiseGenerator.generatorSettings()
			);

			return dimensions.replaceOverworldGenerator(registries, multibiomeGenerator);
		});
	}
}
