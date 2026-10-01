package com.protoano.granules.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class GranulesSounds {
    public static final SoundEvent PET_VARIANT = register("entity.pet.variant");
    public static final SoundEvent PET_BED_OPEN = register("block.pet_bed.open");
    public static final SoundEvent SOIL_ADD = register("block.planter.soil_add");
    public static final SoundEvent LITTER_STEP = register("block.litter_clump.step");
    public static final SoundEvent LOCK = register("item.iron_lock.lock");
    public static final SoundEvent UNLOCK = register("item.iron_lock.unlock");
    public static final SoundEvent BOMB_THROW = register("entity.bomb.throw");
    public static final SoundEvent BOMB_PRIME = register("entity.bomb.prime");
    public static final SoundEvent FILTER_APPLY = register("entity.end_crystal.filter_apply");
    public static final SoundEvent FILTER_REMOVE = register("entity.end_crystal.filter_remove");
    public static final SoundEvent ARROW_REEL = register("entity.fletchers_arrow.reel");
    public static final SoundEvent ARROW_ROCKET = register("entity.fletchers_arrow.rocket");
    public static final SoundEvent FLETCH = register("block.fletching_table.craft");
    public static final SoundEvent AMETHYST_ECHO = register("entity.fletchers_arrow.amethyst_echo");
    public static final SoundEvent PET_TELEPORT = register("entity.pet.teleport");
    public static final SoundEvent PET_REVIVE = register("entity.pet.revive");
    public static final SoundEvent BREAD_RETURN = register("item.bread_crumbs.return");
    public static final SoundEvent TOME_UNLOCK = register("item.enchanted_tome.unlock");
    public static final SoundEvent SATCHEL_OPEN = register("item.somnosatchel.open");
    public static final SoundEvent SOIL_REMOVE = register("block.planter.soil_remove");
    public static final SoundEvent CROP_REMOVE = register("block.planter.crop_remove");
    public static final SoundEvent CROP_HARVEST = register("block.planter.harvest");
    public static final SoundEvent BARREL_SEAL = register("block.barrel.seal");
    public static final SoundEvent BARREL_UNSEAL = register("block.barrel.unseal");
    public static final SoundEvent STAND_ARMS = register("entity.armor_stand.arms");
    public static final SoundEvent OBSIDIAN_DRAIN = register("block.crying_obsidian.drain");

    private GranulesSounds() {
    }

    public static void initialize() {
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("granules", path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
