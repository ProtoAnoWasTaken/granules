package com.protoano.granules.aspen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class AspenContent {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Block LOG = register("aspen_log", Blocks.BIRCH_LOG, RotatedPillarBlock::new);
    public static final Block WOOD = register("aspen_wood", Blocks.BIRCH_WOOD, RotatedPillarBlock::new);
    public static final Block STRIPPED_LOG = register("stripped_aspen_log", Blocks.STRIPPED_BIRCH_LOG, RotatedPillarBlock::new);
    public static final Block STRIPPED_WOOD = register("stripped_aspen_wood", Blocks.STRIPPED_BIRCH_WOOD, RotatedPillarBlock::new);
    public static final Block PLANKS = register("aspen_planks", Blocks.BIRCH_PLANKS, Block::new);
    public static final Block LEAVES = register("aspen_leaves", Blocks.BIRCH_LEAVES, AspenLeavesBlock::new);
    public static final AspenSaplingBlock SAPLING = (AspenSaplingBlock) register("aspen_sapling", Blocks.BIRCH_SAPLING, AspenSaplingBlock::new);
    public static final Block STAIRS = register("aspen_stairs", Blocks.BIRCH_STAIRS, properties -> new StairBlock(PLANKS.defaultBlockState(), properties));
    public static final Block SLAB = register("aspen_slab", Blocks.BIRCH_SLAB, SlabBlock::new);
    public static final Block FENCE = register("aspen_fence", Blocks.BIRCH_FENCE, FenceBlock::new);
    public static final Block FENCE_GATE = register("aspen_fence_gate", Blocks.BIRCH_FENCE_GATE, properties -> new FenceGateBlock(WoodType.BIRCH, properties));
    public static final Block DOOR = register("aspen_door", Blocks.BIRCH_DOOR, properties -> new DoorBlock(BlockSetType.BIRCH, properties));
    public static final Block TRAPDOOR = register("aspen_trapdoor", Blocks.BIRCH_TRAPDOOR, properties -> new TrapDoorBlock(BlockSetType.BIRCH, properties));
    public static final Block BUTTON = register("aspen_button", Blocks.BIRCH_BUTTON, properties -> new ButtonBlock(BlockSetType.BIRCH, 30, properties));
    public static final Block PRESSURE_PLATE = register("aspen_pressure_plate", Blocks.BIRCH_PRESSURE_PLATE, properties -> new PressurePlateBlock(BlockSetType.BIRCH, properties));
    public static final Block SIGN = registerWithoutItem("aspen_sign", Blocks.BIRCH_SIGN, properties -> new StandingSignBlock(WoodType.BIRCH, properties));
    public static final Block WALL_SIGN = registerWithoutItem("aspen_wall_sign", Blocks.BIRCH_WALL_SIGN, properties -> new WallSignBlock(WoodType.BIRCH, properties.overrideLootTable(SIGN.getLootTable())));
    public static final Block SHELF = register("aspen_shelf", Blocks.BIRCH_SHELF, ShelfBlock::new);
    public static final Block HANGING_SIGN = registerWithoutItem("aspen_hanging_sign", Blocks.BIRCH_HANGING_SIGN, properties -> new CeilingHangingSignBlock(WoodType.BIRCH, properties));
    public static final Block WALL_HANGING_SIGN = registerWithoutItem("aspen_wall_hanging_sign", Blocks.BIRCH_WALL_HANGING_SIGN, properties -> new WallHangingSignBlock(WoodType.BIRCH, properties.overrideLootTable(HANGING_SIGN.getLootTable())));
    public static final Block POTTED_SAPLING = registerWithoutItem("potted_aspen_sapling", Blocks.POTTED_BIRCH_SAPLING, properties -> new FlowerPotBlock(SAPLING, properties));
    public static final EntityType<Boat> BOAT_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("aspen_boat"),
        EntityType.Builder.<Boat>of((type, level) -> new Boat(type, level, AspenContent::boatItem), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id("aspen_boat"))));
    public static final EntityType<ChestBoat> CHEST_BOAT_ENTITY = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("aspen_chest_boat"),
        EntityType.Builder.<ChestBoat>of((type, level) -> new ChestBoat(type, level, AspenContent::chestBoatItem), MobCategory.MISC)
            .noLootTable()
            .sized(1.375F, 0.5625F)
            .eyeHeight(0.5625F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id("aspen_chest_boat"))));
    public static final Item BOAT = Registry.register(BuiltInRegistries.ITEM, id("aspen_boat"),
        new BoatItem(BOAT_ENTITY, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("aspen_boat"))).stacksTo(1)));
    public static final Item CHEST_BOAT = Registry.register(BuiltInRegistries.ITEM, id("aspen_chest_boat"),
        new BoatItem(CHEST_BOAT_ENTITY, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("aspen_chest_boat"))).stacksTo(1)));

    private AspenContent() {
    }

    public static void initialize() {
        SignItem sign = new SignItem(SIGN, WALL_SIGN, itemProperties("aspen_sign").stacksTo(16));
        Registry.register(BuiltInRegistries.ITEM, id("aspen_sign"), sign);
        sign.registerBlocks(Item.BY_BLOCK, sign);
        HangingSignItem hangingSign = new HangingSignItem(HANGING_SIGN, WALL_HANGING_SIGN, itemProperties("aspen_hanging_sign").stacksTo(16));
        Registry.register(BuiltInRegistries.ITEM, id("aspen_hanging_sign"), hangingSign);
        hangingSign.registerBlocks(Item.BY_BLOCK, hangingSign);
        BlockEntityTypes.SIGN.addValidBlock(SIGN);
        BlockEntityTypes.SIGN.addValidBlock(WALL_SIGN);
        BlockEntityTypes.SHELF.addValidBlock(SHELF);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(HANGING_SIGN);
        BlockEntityTypes.HANGING_SIGN.addValidBlock(WALL_HANGING_SIGN);
        DispenserBlock.registerBehavior(BOAT, new BoatDispenseItemBehavior(BOAT_ENTITY));
        DispenserBlock.registerBehavior(CHEST_BOAT, new BoatDispenseItemBehavior(CHEST_BOAT_ENTITY));
        StrippableBlockRegistry.register(LOG, STRIPPED_LOG);
        StrippableBlockRegistry.register(WOOD, STRIPPED_WOOD);
        for (Block block : BLOCKS.values()) {
            if (block != SAPLING && block != POTTED_SAPLING) {
                int burnChance = block == LEAVES ? 60 : block instanceof RotatedPillarBlock ? 5 : 20;
                FlammableBlockRegistry.getDefaultInstance().add(block, block == LEAVES ? 30 : 5, burnChance);
            }
        }
        ComposterBlock.COMPOSTABLES.put(SAPLING.asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(LEAVES.asItem(), 0.3F);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            for (Block block : BLOCKS.values()) {
                if (block != SAPLING && block != LEAVES && block != WALL_SIGN && block != WALL_HANGING_SIGN && block != POTTED_SAPLING) {
                    entries.accept(block);
                }
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.accept(LOG);
            entries.accept(LEAVES);
            entries.accept(SAPLING);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(BOAT);
            entries.accept(CHEST_BOAT);
        });
    }

    private static Item boatItem() {
        return BOAT;
    }

    private static Item chestBoatItem() {
        return CHEST_BOAT;
    }

    private static Block register(String name, Block template, Function<BlockBehaviour.Properties, Block> factory) {
        Block block = registerWithoutItem(name, template, factory);
        BlockItem item = block instanceof DoorBlock
            ? new DoubleHighBlockItem(block, itemProperties(name))
            : new BlockItem(block, itemProperties(name));
        Registry.register(BuiltInRegistries.ITEM, id(name), item);
        item.registerBlocks(Item.BY_BLOCK, item);
        return block;
    }

    private static Block registerWithoutItem(String name, Block template, Function<BlockBehaviour.Properties, Block> factory) {
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(template).setId(ResourceKey.create(Registries.BLOCK, id(name))));
        Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        BLOCKS.put(name, block);
        return block;
    }

    private static Item.Properties itemProperties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name))).useBlockDescriptionPrefix();
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("granules", name);
    }
}
