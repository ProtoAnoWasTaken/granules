package com.protoano.granules.block;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class LitterClumpContent {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("granules", "litter_clump");
    public static final Block BLOCK = Registry.register(
        BuiltInRegistries.BLOCK,
        ID,
        new TintedParticleLeavesBlock(0.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
            .setId(ResourceKey.create(Registries.BLOCK, ID))
            .sound(new net.minecraft.world.level.block.SoundType(1.0F, 1.0F,
                net.minecraft.world.level.block.SoundType.GRASS.getBreakSound(),
                com.protoano.granules.sound.GranulesSounds.LITTER_STEP,
                net.minecraft.world.level.block.SoundType.GRASS.getPlaceSound(),
                net.minecraft.world.level.block.SoundType.GRASS.getHitSound(),
                net.minecraft.world.level.block.SoundType.GRASS.getFallSound()))
            .mapColor(MapColor.COLOR_BROWN))
    );
    public static final BlockItem ITEM = Registry.register(
        BuiltInRegistries.ITEM,
        ID,
        new BlockItem(BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix())
    );

    private LitterClumpContent() {
    }

    public static void initialize() {
        FlammableBlockRegistry.getDefaultInstance().add(BLOCK, 30, 60);
        ComposterBlock.COMPOSTABLES.put(ITEM, 0.75F);
        FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(ITEM, context.baseSmeltTime() * 9 / 2);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.insertAfter(Items.LEAF_LITTER, ITEM);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SEARCH).register(entries -> {
            entries.insertAfter(Items.LEAF_LITTER, ITEM);
        });
    }
}
