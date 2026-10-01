package com.protoano.granules.block;

import java.util.LinkedHashMap;
import java.util.Map;
import com.protoano.granules.aspen.AspenContent;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ChiseledLogContent {
    public static final Map<Block, Block> VARIANTS = new LinkedHashMap<>();
    public static final Map<Block, Block> STUMPS = new LinkedHashMap<>();
    public static final Map<Block, Block> STRIPPED_VARIANTS = new LinkedHashMap<>();
    public static final Map<Block, Block> STRIPPED_STUMPS = new LinkedHashMap<>();
    public static final Map<Block, Block> EXTRA_VARIANTS = new LinkedHashMap<>();

    private ChiseledLogContent() {
    }

    public static void initialize() {
        register(Blocks.OAK_LOG);
        register(Blocks.SPRUCE_LOG);
        register(Blocks.BIRCH_LOG);
        register(Blocks.JUNGLE_LOG);
        register(Blocks.ACACIA_LOG);
        register(Blocks.DARK_OAK_LOG);
        register(Blocks.MANGROVE_LOG);
        register(Blocks.CHERRY_LOG);
        register(Blocks.PALE_OAK_LOG);
        register(Blocks.CRIMSON_STEM);
        register(Blocks.WARPED_STEM);
        register(AspenContent.LOG);
        for (Block base : new Block[] {Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
            Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
            Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_PALE_OAK_LOG,
            Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM, AspenContent.STRIPPED_LOG}) {
            register(base);
        }
        EXTRA_VARIANTS.put(Blocks.STRIPPED_BAMBOO_BLOCK, registerChiseled(Blocks.STRIPPED_BAMBOO_BLOCK, "chiseled_bamboo_block"));
        EXTRA_VARIANTS.put(com.protoano.granules.chorus.ChorusContent.BLOCK,
            registerChiseled(com.protoano.granules.chorus.ChorusContent.BLOCK, "chiseled_chorus_block"));
        net.fabricmc.fabric.api.registry.FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(EXTRA_VARIANTS.get(com.protoano.granules.chorus.ChorusContent.BLOCK), context.baseSmeltTime() * 3 / 2);
        });
        Block[] stripped = STRIPPED_VARIANTS.keySet().toArray(Block[]::new);
        int index = 0;
        for (Block base : VARIANTS.keySet()) {
            net.fabricmc.fabric.api.registry.StrippableBlockRegistry.register(VARIANTS.get(base), STRIPPED_VARIANTS.get(stripped[index]));
            net.fabricmc.fabric.api.registry.StrippableBlockRegistry.registerCopyState(STUMPS.get(base), STRIPPED_STUMPS.get(stripped[index]));
            index++;
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            VARIANTS.forEach((base, chiseled) -> {
                entries.insertAfter(base, chiseled);
                entries.insertAfter(chiseled, STUMPS.get(base));
            });
            STRIPPED_VARIANTS.forEach((base, chiseled) -> {
                entries.insertAfter(base, chiseled);
                entries.insertAfter(chiseled, STRIPPED_STUMPS.get(base));
            });
            EXTRA_VARIANTS.forEach((base, chiseled) -> entries.insertAfter(base, chiseled));
        });
    }

    private static void register(Block base) {
        String path = BuiltInRegistries.BLOCK.getKey(base).getPath();
        Block block = registerChiseled(base, "chiseled_" + path);
        boolean stripped = path.startsWith("stripped_");
        (stripped ? STRIPPED_VARIANTS : VARIANTS).put(base, block);
        String species = path.replace("_log", "").replace("_stem", "");
        Identifier stumpId = Identifier.fromNamespaceAndPath("granules", species + "_stump");
        Block stump = Registry.register(BuiltInRegistries.BLOCK, stumpId, new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.ofFullCopy(base)
            .mapColor(base.defaultBlockState().getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO))
            .setId(ResourceKey.create(Registries.BLOCK, stumpId))));
        Registry.register(BuiltInRegistries.ITEM, stumpId, new BlockItem(stump, new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, stumpId)).useBlockDescriptionPrefix()));
        (stripped ? STRIPPED_STUMPS : STUMPS).put(base, stump);
        if (!path.endsWith("_stem")) {
            FlammableBlockRegistry.getDefaultInstance().add(stump, 5, 5);
        }
    }

    private static Block registerChiseled(Block base, String path) {
        Identifier id = Identifier.fromNamespaceAndPath("granules", path);
        Block block = Registry.register(BuiltInRegistries.BLOCK, id, new Block(BlockBehaviour.Properties.ofFullCopy(base)
            .mapColor(base.defaultBlockState().getMapColor(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO))
            .setId(ResourceKey.create(Registries.BLOCK, id))));
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
        if (!path.endsWith("_stem")) {
            FlammableBlockRegistry.getDefaultInstance().add(block, 5, 5);
        }
        return block;
    }
}
