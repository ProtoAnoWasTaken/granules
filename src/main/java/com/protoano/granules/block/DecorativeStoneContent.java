package com.protoano.granules.block;

import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class DecorativeStoneContent {
    public static final Block ECHO_SHARD_BLOCK = register("echo_shard_block", false,
        Block::new);
    public static final Block POLISHED_ECHO_SHARD_BLOCK = register("polished_echo_shard_block", false,
        Block::new);
    public static final Block POLISHED_ECHO_SHARD_STAIRS = register("polished_echo_shard_stairs", false,
        properties -> new DecorativeStairBlock(POLISHED_ECHO_SHARD_BLOCK.defaultBlockState(), properties));
    public static final Block POLISHED_ECHO_SHARD_SLAB = register("polished_echo_shard_slab", false,
        SlabBlock::new);
    public static final Block POLISHED_ECHO_SHARD_WALL = register("polished_echo_shard_wall", false,
        WallBlock::new);
    public static final Block ECHO_SHARD_PILLAR = register("echo_shard_pillar", false,
        RotatedPillarBlock::new);
    public static final Block ECHO_BRICKS = register("echo_bricks", false,
        Block::new);
    public static final Block ECHO_BRICK_STAIRS = register("echo_brick_stairs", false,
        properties -> new DecorativeStairBlock(ECHO_BRICKS.defaultBlockState(), properties));
    public static final Block ECHO_BRICK_SLAB = register("echo_brick_slab", false,
        SlabBlock::new);
    public static final Block ECHO_BRICK_WALL = register("echo_brick_wall", false,
        WallBlock::new);
    public static final Block POLISHED_AMETHYST_BLOCK = register("polished_amethyst_block", true,
        AmethystBlock::new);
    public static final Block POLISHED_AMETHYST_STAIRS = register("polished_amethyst_stairs", true,
        properties -> new DecorativeStairBlock(POLISHED_AMETHYST_BLOCK.defaultBlockState(), properties));
    public static final Block POLISHED_AMETHYST_SLAB = register("polished_amethyst_slab", true,
        SlabBlock::new);
    public static final Block POLISHED_AMETHYST_WALL = register("polished_amethyst_wall", true,
        WallBlock::new);
    public static final Block AMETHYST_PILLAR = register("amethyst_pillar", true,
        RotatedPillarBlock::new);
    public static final Block AMETHYST_BRICKS = register("amethyst_bricks", true,
        AmethystBlock::new);
    public static final Block AMETHYST_BRICK_STAIRS = register("amethyst_brick_stairs", true,
        properties -> new DecorativeStairBlock(AMETHYST_BRICKS.defaultBlockState(), properties));
    public static final Block AMETHYST_BRICK_SLAB = register("amethyst_brick_slab", true,
        SlabBlock::new);
    public static final Block AMETHYST_BRICK_WALL = register("amethyst_brick_wall", true,
        WallBlock::new);
    public static final List<Block> ECHO_BLOCKS = List.of(
        ECHO_SHARD_BLOCK,
        POLISHED_ECHO_SHARD_BLOCK,
        POLISHED_ECHO_SHARD_STAIRS,
        POLISHED_ECHO_SHARD_SLAB,
        POLISHED_ECHO_SHARD_WALL,
        ECHO_SHARD_PILLAR,
        ECHO_BRICKS,
        ECHO_BRICK_STAIRS,
        ECHO_BRICK_SLAB,
        ECHO_BRICK_WALL);
    public static final List<Block> AMETHYST_BLOCKS = List.of(
        POLISHED_AMETHYST_BLOCK,
        POLISHED_AMETHYST_STAIRS,
        POLISHED_AMETHYST_SLAB,
        POLISHED_AMETHYST_WALL,
        AMETHYST_PILLAR,
        AMETHYST_BRICKS,
        AMETHYST_BRICK_STAIRS,
        AMETHYST_BRICK_SLAB,
        AMETHYST_BRICK_WALL);

    private DecorativeStoneContent() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            Item previous = Items.CHISELED_TUFF_BRICKS;
            for (Block block : ECHO_BLOCKS) {
                entries.insertAfter(previous, block.asItem());
                previous = block.asItem();
            }
            previous = Items.AMETHYST_BLOCK;
            for (Block block : AMETHYST_BLOCKS) {
                entries.insertAfter(previous, block.asItem());
                previous = block.asItem();
            }
        });
    }

    private static Block register(String name, boolean amethyst, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath("granules", name);
        Block base = amethyst ? Blocks.AMETHYST_BLOCK : Blocks.DEEPSLATE;
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(base)
            .setId(ResourceKey.create(Registries.BLOCK, id));
        if (amethyst) {
            properties.destroyTime(base.defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) * 1.5F);
        }
        Block block = Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(properties));
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
        return block;
    }

    private static final class DecorativeStairBlock extends StairBlock {
        private DecorativeStairBlock(BlockState base, BlockBehaviour.Properties properties) {
            super(base, properties);
        }
    }
}
