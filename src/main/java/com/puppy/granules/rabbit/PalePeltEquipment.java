package com.puppy.granules.rabbit;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.core.Holder;
import net.minecraft.world.level.gameevent.GameEvent;

public final class PalePeltEquipment {
    private static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, id("pale_pelt_items"));
    private static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, id("pale_pelt_repairs"));
    private static final ArmorMaterial MATERIAL = createMaterial();
    public static final Item FUR_BOOTS = registerBoots();
    public static final Item OPERA_GLOVE = registerItem("opera_glove", new Item.Properties().stacksTo(1));
    public static final Item TRAPPER_HAT = registerArmor("trapper_hat", ArmorType.HELMET);

    private PalePeltEquipment() {
    }

    public static void initialize() {
        if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
            return;
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.insertAfter(net.minecraft.world.item.Items.LEATHER_BOOTS, TRAPPER_HAT, FUR_BOOTS);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.insertBefore(net.minecraft.world.item.Items.ELYTRA, OPERA_GLOVE);
        });
    }

    public static boolean wearsBoots(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.FEET).is(FUR_BOOTS);
    }

    public static boolean wearsHat(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(TRAPPER_HAT);
    }

    public static boolean holdsGlove(net.minecraft.world.entity.player.Player player) {
        return player.getOffhandItem().is(OPERA_GLOVE);
    }

    public static boolean insulatesMainHand(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getOffhandItem().is(OPERA_GLOVE) && !entity.getMainHandItem().has(DataComponents.TOOL);
    }

    public static boolean isHandheldEvent(Holder<GameEvent> event) {
        return event == GameEvent.BLOCK_ACTIVATE
            || event == GameEvent.BLOCK_CHANGE
            || event == GameEvent.BLOCK_CLOSE
            || event == GameEvent.BLOCK_DESTROY
            || event == GameEvent.BLOCK_OPEN
            || event == GameEvent.BLOCK_PLACE
            || event == GameEvent.CONTAINER_CLOSE
            || event == GameEvent.CONTAINER_OPEN
            || event == GameEvent.DRINK
            || event == GameEvent.EAT
            || event == GameEvent.ENTITY_INTERACT
            || event == GameEvent.ENTITY_PLACE
            || event == GameEvent.FLUID_PICKUP
            || event == GameEvent.FLUID_PLACE
            || event == GameEvent.ITEM_INTERACT_FINISH
            || event == GameEvent.ITEM_INTERACT_START
            || event == GameEvent.PROJECTILE_SHOOT
            || event == GameEvent.SHEAR;
    }

    private static ArmorMaterial createMaterial() {
        ArmorMaterial leather = ArmorMaterials.LEATHER;
        return new ArmorMaterial(
            leather.durability(),
            leather.defense(),
            ArmorMaterials.GOLD.enchantmentValue(),
            leather.equipSound(),
            leather.toughness(),
            leather.knockbackResistance(),
            REPAIRS,
            ASSET
        );
    }

    private static Item registerBoots() {
        Identifier id = id("fur_boots");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        ItemAttributeModifiers attributes = MATERIAL.createAttributes(ArmorType.BOOTS)
            .withModifierAdded(
                Attributes.JUMP_STRENGTH,
                new AttributeModifier(id("fur_boots_jump"), 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                EquipmentSlotGroup.FEET
            )
            .withModifierAdded(
                Attributes.SAFE_FALL_DISTANCE,
                new AttributeModifier(id("fur_boots_safe_fall"), 0.75, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.FEET
            )
            .withModifierAdded(
                Attributes.AIR_DRAG_MODIFIER,
                new AttributeModifier(id("fur_boots_air_drag"), -1.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.FEET
            );
        Item.Properties properties = new Item.Properties()
            .setId(key)
            .humanoidArmor(MATERIAL, ArmorType.BOOTS)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(properties));
    }

    private static Item registerArmor(String path, ArmorType type) {
        Identifier id = id(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(key).humanoidArmor(MATERIAL, type)));
    }

    private static Item registerItem(String path, Item.Properties properties) {
        Identifier id = id(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(properties.setId(key)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path);
    }
}
