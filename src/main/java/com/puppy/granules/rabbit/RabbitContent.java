package com.puppy.granules.rabbit;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.InteractionResult;

public final class RabbitContent {
    public static final Item PALE_PELT = registerItem("pale_pelt");
    public static final SpawnEggItem KILLER_BUNNY_SPAWN_EGG = registerSpawnEgg();

    private RabbitContent() {
    }

    public static void initialize() {
        PalePeltEquipment.initialize();
        if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
            return;
        }
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (
                entity instanceof Rabbit rabbit
                    && rabbit.getVariant() == Rabbit.Variant.EVIL
                    && rabbit instanceof KillerRabbitAccess access
                    && access.granules$isTamedKillerRabbit()
                    && player.getUUID().equals(access.granules$ownerUuid())
            ) {
                if (!level.isClientSide()) {
                    access.granules$toggleSitting(player);
                }
                return level.isClientSide()
                    ? InteractionResult.SUCCESS
                    : InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.insertAfter(Items.RABBIT_HIDE, PALE_PELT);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.insertAfter(Items.RABBIT_SPAWN_EGG, KILLER_BUNNY_SPAWN_EGG);
        });
    }

    private static Item registerItem(String path) {
        Identifier id = id(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(key)));
    }

    private static SpawnEggItem registerSpawnEgg() {
        Identifier id = id("killer_bunny_spawn_egg");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties()
            .setId(key)
            .spawnEgg(EntityTypes.RABBIT)
            .component(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        return Registry.register(BuiltInRegistries.ITEM, id, new SpawnEggItem(properties));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path);
    }
}
