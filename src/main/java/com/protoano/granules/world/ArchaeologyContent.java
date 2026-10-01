package com.protoano.granules.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;

public final class ArchaeologyContent {
    private static final Identifier SAND_ID = id("suspicious_red_sand");
    public static final BrushableBlock SUSPICIOUS_RED_SAND = Registry.register(
        BuiltInRegistries.BLOCK,
        SAND_ID,
        new BrushableBlock(Blocks.RED_SAND, SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND_COMPLETED,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_SAND)
                .setId(ResourceKey.create(Registries.BLOCK, SAND_ID))
                .mapColor(MapColor.COLOR_ORANGE))
    );
    public static final BlockItem SUSPICIOUS_RED_SAND_ITEM = Registry.register(
        BuiltInRegistries.ITEM,
        SAND_ID,
        new BlockItem(SUSPICIOUS_RED_SAND, new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, SAND_ID))
            .useBlockDescriptionPrefix())
    );
    public static final Feature<?> BADLANDS_WELL = Registry.register(BuiltInRegistries.FEATURE, id("badlands_well"), new BadlandsWellFeature());
    public static final Feature<?> SHORE_ARCHAEOLOGY = Registry.register(BuiltInRegistries.FEATURE, id("shore_archaeology"), new ShoreArchaeologyFeature());
    public static final ResourceKey<LootTable> SHORE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, id("archaeology/shore"));

    private ArchaeologyContent() {
    }

    public static void initialize() {
        ((FabricBlockEntityType) BlockEntityTypes.BRUSHABLE_BLOCK).addValidBlock(SUSPICIOUS_RED_SAND);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.insertAfter(Items.SUSPICIOUS_SAND, SUSPICIOUS_RED_SAND_ITEM);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SEARCH).register(entries -> {
            entries.insertAfter(Items.SUSPICIOUS_SAND, SUSPICIOUS_RED_SAND_ITEM);
        });
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_BADLANDS), GenerationStep.Decoration.SURFACE_STRUCTURES, placed("badlands_well"));
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD).or(BiomeSelectors.foundInOverworld()),
            GenerationStep.Decoration.TOP_LAYER_MODIFICATION, placed("shore_archaeology"));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, id(name));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("granules", name);
    }
}
