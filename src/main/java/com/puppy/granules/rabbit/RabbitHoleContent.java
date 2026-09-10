package com.puppy.granules.rabbit;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;
import com.puppy.granules.world.RabbitHoleFeature;
import com.puppy.granules.world.BurrowRabbitHoleFeature;
import com.puppy.granules.world.BurrowSurfaceFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.sounds.SoundEvent;

import java.util.Set;

public final class RabbitHoleContent {
    public static final ResourceKey<net.minecraft.world.level.Level> BURROW = ResourceKey.create(
        Registries.DIMENSION,
        id("the_burrow")
    );
    public static final SoundEvent SCOOP_SOUND = registerSound("rabbit_hole.scoop");
    public static final SoundEvent PRIME_SOUND = registerSound("rabbit_hole.prime");
    public static final SoundEvent ENTER_SOUND = registerSound("rabbit_hole.enter");
    public static final SoundEvent EMERGE_SOUND = registerSound("rabbit_hole.emerge");
    public static final RabbitHoleBlock BLOCK = Registry.register(
        BuiltInRegistries.BLOCK,
        id("rabbit_hole"),
        new RabbitHoleBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
                .setId(ResourceKey.create(Registries.BLOCK, id("rabbit_hole")))
                .mapColor(MapColor.NONE)
                .noOcclusion()
                .lightLevel(state -> state.getValue(RabbitHoleBlock.PRIMED) ? 6 : 0)
        )
    );
    public static final BlockItem ITEM = Registry.register(
        BuiltInRegistries.ITEM,
        id("rabbit_hole"),
        new BlockItem(BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("rabbit_hole"))))
    );
    public static final BlockEntityType<RabbitHoleBlockEntity> BLOCK_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        id("rabbit_hole"),
        new BlockEntityType<>(RabbitHoleBlockEntity::new, Set.of(BLOCK))
    );
    public static final Feature<NoneFeatureConfiguration> FEATURE = Registry.register(
        BuiltInRegistries.FEATURE,
        id("rabbit_hole"),
        new RabbitHoleFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> BURROW_FEATURE = Registry.register(
        BuiltInRegistries.FEATURE,
        id("burrow_rabbit_hole"),
        new BurrowRabbitHoleFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final Feature<NoneFeatureConfiguration> BURROW_SURFACE_FEATURE = Registry.register(
        BuiltInRegistries.FEATURE,
        id("burrow_surface"),
        new BurrowSurfaceFeature(NoneFeatureConfiguration.CODEC)
    );
    private static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(
        Registries.PLACED_FEATURE,
        id("rabbit_hole")
    );

    private RabbitHoleContent() {
    }

    public static void initialize() {
        if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
            return;
        }
        BiomeModifications.addFeature(
            BiomeSelectors.tag(BiomeTags.IS_FOREST),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            PLACED_FEATURE
        );
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.insertBefore(Items.END_CRYSTAL, ITEM);
        });
    }

    private static SoundEvent registerSound(String path) {
        Identifier id = id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path);
    }
}
