package com.protoano.granules.chorus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Raft;
import net.minecraft.world.entity.vehicle.boat.ChestRaft;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ChorusContent {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final SoundType SOUND = Blocks.CHORUS_PLANT.defaultBlockState().getSoundType();
    public static final BlockSetType SET_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
        .register(id("chorus"));
    public static final WoodType WOOD_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder.copyOf(WoodType.BAMBOO)
        .soundType(SOUND).hangingSignSoundType(SOUND).register(id("chorus"), SET_TYPE);
    public static final Block BLOCK = register("chorus_block", Blocks.BAMBOO_BLOCK, RotatedPillarBlock::new);
    public static final Block PLANKS = register("chorus_planks", Blocks.BAMBOO_PLANKS, Block::new);
    public static final Block MOSAIC = register("chorus_mosaic", Blocks.BAMBOO_MOSAIC, Block::new);
    public static final Block STAIRS = register("chorus_stairs", Blocks.BAMBOO_STAIRS, properties -> new StairBlock(PLANKS.defaultBlockState(), properties));
    public static final Block SLAB = register("chorus_slab", Blocks.BAMBOO_SLAB, SlabBlock::new);
    public static final Block MOSAIC_STAIRS = register("chorus_mosaic_stairs", Blocks.BAMBOO_MOSAIC_STAIRS, properties -> new StairBlock(MOSAIC.defaultBlockState(), properties));
    public static final Block MOSAIC_SLAB = register("chorus_mosaic_slab", Blocks.BAMBOO_MOSAIC_SLAB, SlabBlock::new);
    public static final Block FENCE = register("chorus_fence", Blocks.BAMBOO_FENCE, FenceBlock::new);
    public static final Block FENCE_GATE = register("chorus_fence_gate", Blocks.BAMBOO_FENCE_GATE, properties -> new FenceGateBlock(WOOD_TYPE, properties));
    public static final Block DOOR = register("chorus_door", Blocks.BAMBOO_DOOR, properties -> new DoorBlock(SET_TYPE, properties));
    public static final Block TRAPDOOR = register("chorus_trapdoor", Blocks.BAMBOO_TRAPDOOR, properties -> new TrapDoorBlock(SET_TYPE, properties));
    public static final Block BUTTON = register("chorus_button", Blocks.BAMBOO_BUTTON, properties -> new ButtonBlock(SET_TYPE, 30, properties));
    public static final Block PRESSURE_PLATE = register("chorus_pressure_plate", Blocks.BAMBOO_PRESSURE_PLATE, properties -> new PressurePlateBlock(SET_TYPE, properties));
    public static final Block SHELF = register("chorus_shelf", Blocks.BAMBOO_SHELF, ShelfBlock::new);
    public static final Block SIGN = registerWithoutItem("chorus_sign", Blocks.BAMBOO_SIGN, properties -> new StandingSignBlock(WOOD_TYPE, properties));
    public static final Block WALL_SIGN = registerWithoutItem("chorus_wall_sign", Blocks.BAMBOO_WALL_SIGN, properties -> new WallSignBlock(WOOD_TYPE, properties.overrideLootTable(SIGN.getLootTable())));
    public static final Block HANGING_SIGN = registerWithoutItem("chorus_hanging_sign", Blocks.BAMBOO_HANGING_SIGN, properties -> new CeilingHangingSignBlock(WOOD_TYPE, properties));
    public static final Block WALL_HANGING_SIGN = registerWithoutItem("chorus_wall_hanging_sign", Blocks.BAMBOO_WALL_HANGING_SIGN, properties -> new WallHangingSignBlock(WOOD_TYPE, properties.overrideLootTable(HANGING_SIGN.getLootTable())));
    public static final EntityType<Raft> RAFT_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("chorus_raft"),
        EntityType.Builder.<Raft>of((type, level) -> new Raft(type, level, ChorusContent::raftItem), MobCategory.MISC)
            .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id("chorus_raft"))));
    public static final EntityType<ChestRaft> CHEST_RAFT_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("chorus_chest_raft"),
        EntityType.Builder.<ChestRaft>of((type, level) -> new ChestRaft(type, level, ChorusContent::chestRaftItem), MobCategory.MISC)
            .noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id("chorus_chest_raft"))));
    public static final Item RAFT = Registry.register(BuiltInRegistries.ITEM, id("chorus_raft"),
        new BoatItem(RAFT_ENTITY, itemProperties("chorus_raft").stacksTo(1)));
    public static final Item CHEST_RAFT = Registry.register(BuiltInRegistries.ITEM, id("chorus_chest_raft"),
        new BoatItem(CHEST_RAFT_ENTITY, itemProperties("chorus_chest_raft").stacksTo(1)));

    private ChorusContent() {
    }

    public static void initialize() {
        SignItem sign = new SignItem(SIGN, WALL_SIGN, itemProperties("chorus_sign").useBlockDescriptionPrefix().stacksTo(16));
        Registry.register(BuiltInRegistries.ITEM, id("chorus_sign"), sign);
        sign.registerBlocks(Item.BY_BLOCK, sign);
        HangingSignItem hangingSign = new HangingSignItem(HANGING_SIGN, WALL_HANGING_SIGN, itemProperties("chorus_hanging_sign").useBlockDescriptionPrefix().stacksTo(16));
        Registry.register(BuiltInRegistries.ITEM, id("chorus_hanging_sign"), hangingSign);
        hangingSign.registerBlocks(Item.BY_BLOCK, hangingSign);
        BlockEntityTypes.SIGN.addValidBlock(SIGN);
        BlockEntityTypes.SIGN.addValidBlock(WALL_SIGN);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(HANGING_SIGN);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(WALL_HANGING_SIGN);
        BlockEntityTypes.SHELF.addValidBlock(SHELF);
        DispenserBlock.registerBehavior(RAFT, new BoatDispenseItemBehavior(RAFT_ENTITY));
        DispenserBlock.registerBehavior(CHEST_RAFT, new BoatDispenseItemBehavior(CHEST_RAFT_ENTITY));
        net.fabricmc.fabric.api.registry.FuelValueEvents.BUILD.register((builder, context) -> {
            builder.add(BLOCK, context.baseSmeltTime() * 3 / 2);
            builder.add(MOSAIC, context.baseSmeltTime() * 3 / 2);
            builder.add(MOSAIC_STAIRS, context.baseSmeltTime() * 3 / 2);
            builder.add(MOSAIC_SLAB, context.baseSmeltTime() * 3 / 4);
        });
        for (Block block : BLOCKS.values()) {
            FlammableBlockRegistry.getDefaultInstance().add(block, 5, block == BLOCK ? 5 : 20);
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            for (Block block : BLOCKS.values()) {
                if (block != WALL_SIGN && block != WALL_HANGING_SIGN) {
                    entries.accept(block);
                }
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(RAFT);
            entries.accept(CHEST_RAFT);
        });
    }

    private static Item raftItem() {
        return RAFT;
    }

    private static Item chestRaftItem() {
        return CHEST_RAFT;
    }

    private static Block register(String name, Block template, Function<BlockBehaviour.Properties, Block> factory) {
        Block block = registerWithoutItem(name, template, factory);
        Item.Properties properties = itemProperties(name).useBlockDescriptionPrefix();
        BlockItem item = block instanceof DoorBlock ? new DoubleHighBlockItem(block, properties) : new BlockItem(block, properties);
        Registry.register(BuiltInRegistries.ITEM, id(name), item);
        item.registerBlocks(Item.BY_BLOCK, item);
        return block;
    }

    private static Block registerWithoutItem(String name, Block template, Function<BlockBehaviour.Properties, Block> factory) {
        boolean normalWood = template == Blocks.BAMBOO_DOOR || template == Blocks.BAMBOO_TRAPDOOR
            || template == Blocks.BAMBOO_PRESSURE_PLATE || template == Blocks.BAMBOO_BUTTON;
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(template).sound(normalWood ? SoundType.WOOD : SOUND)
            .setId(ResourceKey.create(Registries.BLOCK, id(name))));
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        BLOCKS.put(name, block);
        return block;
    }

    private static Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("granules", name);
    }
}
