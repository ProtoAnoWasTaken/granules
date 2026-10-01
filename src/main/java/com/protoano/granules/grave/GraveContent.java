package com.protoano.granules.grave;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class GraveContent {
    public static final Identifier BURIAL_ID = Identifier.fromNamespaceAndPath("granules", "burial_preview");
    public static final EntityType<net.minecraft.world.entity.Display.ItemDisplay> BURIAL = Registry.register(BuiltInRegistries.ENTITY_TYPE, BURIAL_ID,
        EntityType.Builder.<net.minecraft.world.entity.Display.ItemDisplay>of(net.minecraft.world.entity.Display.ItemDisplay::new, MobCategory.MISC)
            .sized(0, 0).noSave().noSummon().noLootTable().fireImmune().clientTrackingRange(3).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, BURIAL_ID)));
    public static final Identifier SHOVEL_ID = Identifier.fromNamespaceAndPath("granules", "grave_shovel");
    public static final GraveShovelItem SHOVEL = Registry.register(BuiltInRegistries.ITEM, SHOVEL_ID,
        new GraveShovelItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SHOVEL_ID))));
    public static final Identifier GHOST_ID = Identifier.fromNamespaceAndPath("granules", "grave_ghost");
    public static final EntityType<GraveGhostEntity> GHOST = Registry.register(BuiltInRegistries.ENTITY_TYPE, GHOST_ID,
        EntityType.Builder.<GraveGhostEntity>of(GraveGhostEntity::create, MobCategory.MISC)
            .sized(0.6F, 1.8F).eyeHeight(1.62F).noSave().noSummon().noLootTable().fireImmune()
            .clientTrackingRange(3).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, GHOST_ID)));

    private GraveContent() {
    }

    public static void initialize() {
        GraveDiscovery.initialize();
        FabricDefaultAttributeRegistry.register(GHOST, LivingEntity.createLivingAttributes());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(SHOVEL));
        GraveRescue.initialize();
    }
}
