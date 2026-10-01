package com.protoano.granules.enchantment;

import java.util.Set;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import com.protoano.granules.world.LuckAdjustedChance;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class TreasureSwordEnchantments {
    private static final Set<String> TRIAL_TABLES = Set.of("entrance", "corridor", "intersection",
        "intersection_barrel", "supply", "reward", "reward_ominous");

    private TreasureSwordEnchantments() {
    }

    public static void initialize() {
        Hearthearth.initialize();
        LootTableEvents.MODIFY.register((key, table, source, registries) -> {
            if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.TREASURE_SWORDS)) {
                return;
            }
            if (!source.isBuiltin() || !key.identifier().getNamespace().equals("minecraft")) {
                return;
            }
            String path = key.identifier().getPath();
            if (path.equals("chests/nether_bridge")) {
                addBook(table, registries, GranulesEnchantments.BUTCHERY, com.protoano.granules.config.BalanceConfig.Setting.BUTCHERY_CHANCE.floatValue(), 5);
            }
            if (path.startsWith("chests/trial_chambers/")
                    && TRIAL_TABLES.contains(path.substring("chests/trial_chambers/".length()))) {
                addBook(table, registries, GranulesEnchantments.HEARTHEARTH, com.protoano.granules.config.BalanceConfig.Setting.HEARTHEARTH_CHANCE.floatValue(), 1);
            }
        });
    }

    private static void addBook(LootTable.Builder table, HolderLookup.Provider registries,
                                ResourceKey<Enchantment> key, float chance, int maxLevel) {
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
            .when(LuckAdjustedChance.randomChance(chance))
            .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).apply(new SetEnchantmentsFunction.Builder()
                .withEnchantment(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),
                    UniformGenerator.between(1.0F, maxLevel)))));
    }
}
