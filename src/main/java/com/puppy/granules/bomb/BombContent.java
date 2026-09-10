package com.puppy.granules.bomb;

import com.puppy.granules.GranulesMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

public final class BombContent {
    public static final BombItem DYNAMITE = registerItem("dynamite");
    public static final BombItem DIRT_BOMB = registerItem("dirt_bomb");
    public static final BombItem DRY_BOMB = registerItem("dry_bomb");
    public static final EntityType<BombProjectile> BOMB_ENTITY = registerEntity();

    private BombContent() {
    }

    public static void initialize() {
        DispenserBlock.registerProjectileBehavior(DYNAMITE);
        DispenserBlock.registerProjectileBehavior(DIRT_BOMB);
        DispenserBlock.registerProjectileBehavior(DRY_BOMB);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.insertAfter(Items.TNT, DYNAMITE, DIRT_BOMB, DRY_BOMB);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(DYNAMITE);
            entries.accept(DIRT_BOMB);
            entries.accept(DRY_BOMB);
        });
    }

    private static BombItem registerItem(String path) {
        Identifier id = id(path);
        Item.Properties properties = new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, id))
            .stacksTo(16);
        return Registry.register(BuiltInRegistries.ITEM, id, new BombItem(properties));
    }

    private static EntityType<BombProjectile> registerEntity() {
        Identifier id = id("bomb");
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        EntityType<BombProjectile> type = EntityType.Builder.<BombProjectile>of(BombProjectile::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10)
            .build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path);
    }
}
