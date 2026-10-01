package com.protoano.granules.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class UtilityItems {
    public static final GildedTongsItem TONGS = registerTongs();
    public static final IronLockItem IRON_LOCK = registerLock();

    private UtilityItems() {
    }

    private static GildedTongsItem registerTongs() {
        var id = Identifier.fromNamespaceAndPath("granules", "gilded_tongs");
        return Registry.register(BuiltInRegistries.ITEM, id, new GildedTongsItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).durability(com.protoano.granules.config.BalanceConfig.Setting.TONGS_DURABILITY.intValue()).enchantable(15)));
    }

    private static IronLockItem registerLock() {
        var id = Identifier.fromNamespaceAndPath("granules", "iron_lock");
        return Registry.register(BuiltInRegistries.ITEM, id, new IronLockItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).durability(com.protoano.granules.config.BalanceConfig.Setting.IRON_LOCK_DURABILITY.intValue()).enchantable(14)));
    }

    public static void initialize() {
        PaintingLocks.initialize();
        StateLocks.initialize();
        EntityLocks.initialize();
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(IRON_LOCK, new IronLockDispenseBehavior());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(TONGS);
            entries.accept(IRON_LOCK);
        });
    }
}
