package com.protoano.granules.tea;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;

public final class TeaContent {
    public static final net.minecraft.sounds.SoundEvent HARVEST_SOUND = Registry.register(BuiltInRegistries.SOUND_EVENT, id("block.tea_shrub.harvest"),
        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id("block.tea_shrub.harvest")));
    public static final net.minecraft.world.item.crafting.RecipeSerializer<TeaRecipe> RECIPE_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("tea"),
        new net.minecraft.world.item.crafting.RecipeSerializer<>(TeaRecipe.MAP_CODEC, TeaRecipe.STREAM_CODEC));
    public static final java.util.Map<TeaKind, TeaDrinkItem> DRINKS = new java.util.EnumMap<>(TeaKind.class);
    public static final TeaShrubBlock SHRUB = Registry.register(BuiltInRegistries.BLOCK, id("tea_shrub"),
        new TeaShrubBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).setId(ResourceKey.create(Registries.BLOCK, id("tea_shrub")))));
    public static final BlockItem LEAVES = Registry.register(BuiltInRegistries.ITEM, id("tea_leaves"),
        new BlockItem(SHRUB, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("tea_leaves"))).useItemDescriptionPrefix()));
    public static final ResourceKey<LootTable> HARVEST = ResourceKey.create(Registries.LOOT_TABLE, id("harvest/tea_shrub"));
    public static final ResourceKey<PlacedFeature> PATCH = ResourceKey.create(Registries.PLACED_FEATURE, id("patch_tea_shrub"));

    private TeaContent() {
    }

    public static void initialize() {
        for (TeaKind kind : TeaKind.values()) {
            Item.Properties properties = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(kind.id())))
                .stacksTo(16)
                .usingConvertsTo(Items.GLASS_BOTTLE)
                .component(net.minecraft.core.component.DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumables.DEFAULT_DRINK);
            if (kind.juice()) {
                properties.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(com.protoano.granules.config.BalanceConfig.Setting.JUICE_HUNGER.intValue()).saturationModifier(com.protoano.granules.config.BalanceConfig.Setting.JUICE_SATURATION.floatValue() / Math.max(1, 2 * com.protoano.granules.config.BalanceConfig.Setting.JUICE_HUNGER.intValue())).alwaysEdible().build(),
                    net.minecraft.world.item.component.Consumables.DEFAULT_DRINK);
            }
            DRINKS.put(kind, Registry.register(BuiltInRegistries.ITEM, id(kind.id()), new TeaDrinkItem(kind, properties)));
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            for (TeaKind kind : TeaKind.values()) {
                entries.accept(DRINKS.get(kind));
            }
        });
        LEAVES.registerBlocks(Item.BY_BLOCK, LEAVES);
        ComposterBlock.COMPOSTABLES.put(LEAVES, 0.3F);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
            entries.accept(LEAVES);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(LEAVES);
        });
        if (!com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.TEA)) {
            BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_JUNGLE), GenerationStep.Decoration.VEGETAL_DECORATION, PATCH);
        }
    }

    public static void replaceButcherTrade(Villager villager) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.TEA)) {
            return;
        }
        if (!villager.getVillagerData().profession().is(VillagerProfession.BUTCHER)
            || villager.getVillagerData().level() != 5) {
            return;
        }
        var offers = villager.getOffers();
        for (int index = 0; index < offers.size(); index++) {
            MerchantOffer offer = offers.get(index);
            if (offer.getBaseCostA().is(Items.SWEET_BERRIES) && offer.getResult().is(Items.EMERALD)) {
                if (villager.getRandom().nextFloat() < com.protoano.granules.config.BalanceConfig.Setting.TEA_TRADE_REPLACEMENT_CHANCE.floatValue()) {
                    offers.set(index, new MerchantOffer(new ItemCost(LEAVES, com.protoano.granules.config.BalanceConfig.Setting.TEA_TRADE_LEAVES.intValue()), offer.getResult().copy(), offer.getMaxUses(), offer.getXp(), offer.getPriceMultiplier()));
                }
                return;
            }
        }
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("granules", name);
    }
}
