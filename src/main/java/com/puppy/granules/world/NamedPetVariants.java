package com.puppy.granules.world;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.feline.CatVariants;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariants;

public final class NamedPetVariants {
	private NamedPetVariants() {
	}

	public static void apply(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level) || entity.getCustomName() == null) {
			return;
		}

		String name = entity.getCustomName().getString().trim().toLowerCase(Locale.ROOT);
		boolean applied = false;
		if (entity instanceof Wolf wolf) {
			applied = applyWolf(wolf, name);
		} else if (entity instanceof Cat cat) {
			applied = applyCat(cat, name);
		} else if (entity instanceof Axolotl axolotl) {
			applied = applyAxolotl(axolotl, name);
		}

		if (!applied) {
			return;
		}

		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 20, 0.35, 0.45, 0.35, 0.02);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.2F);
	}

	private static boolean applyWolf(Wolf wolf, String name) {
		switch (name) {
			case "khalki":
				return setWolfVariant(wolf, WolfVariants.RUSTY);
			case "dennis":
				return setWolfVariant(wolf, WolfVariants.PALE);
			case "takaya":
				return setWolfVariant(wolf, WolfVariants.BLACK);
			case "hannah":
				return setWolfVariant(wolf, WolfVariants.STRIPED);
			case "maya":
				return setWolfVariant(wolf, WolfVariants.SNOWY);
			case "romeo":
				return setWolfVariant(wolf, WolfVariants.ASHEN);
			case "lupa":
				return setWolfVariant(wolf, WolfVariants.WOODS);
			case "tait":
				return setWolfVariant(wolf, WolfVariants.SPOTTED);
			case "zhur":
				return setWolfVariant(wolf, WolfVariants.CHESTNUT);
			case "gage":
				return setWolfSoundVariant(wolf, WolfSoundVariants.CLASSIC);
			case "walter":
				return setWolfSoundVariant(wolf, WolfSoundVariants.GRUMPY);
			case "doug":
				return setWolfSoundVariant(wolf, WolfSoundVariants.CUTE);
			case "balltze":
				return setWolfSoundVariant(wolf, WolfSoundVariants.SAD);
			case "iggy":
				return setWolfSoundVariant(wolf, WolfSoundVariants.ANGRY);
			case "ellie":
				return setWolfSoundVariant(wolf, WolfSoundVariants.PUGLIN);
			case "loki":
				return setWolfSoundVariant(wolf, WolfSoundVariants.BIG);
			default:
				return false;
		}
	}

	private static boolean applyCat(Cat cat, String name) {
		switch (name) {
			case "jellie":
				return setCatVariant(cat, CatVariants.JELLIE);
			case "newton":
				return setCatVariant(cat, CatVariants.BLACK);
			case "spider":
				return setCatVariant(cat, CatVariants.ALL_BLACK);
			case "milo":
				return setCatVariant(cat, CatVariants.RED);
			case "buttercup":
				return setCatVariant(cat, CatVariants.TABBY);
			case "georgie":
				return setCatVariant(cat, CatVariants.SIAMESE);
			case "misha":
				return setCatVariant(cat, CatVariants.BRITISH_SHORTHAIR);
			case "panko":
				return setCatVariant(cat, CatVariants.CALICO);
			case "beerus":
				return setCatVariant(cat, CatVariants.PERSIAN);
			case "benjamin":
				return setCatVariant(cat, CatVariants.RAGDOLL);
			case "snowbell":
				return setCatVariant(cat, CatVariants.WHITE);
			default:
				return false;
		}
	}

	private static boolean applyAxolotl(Axolotl axolotl, String name) {
		switch (name) {
			case "gorda":
				axolotl.setComponent(DataComponents.AXOLOTL_VARIANT, Axolotl.Variant.LUCY);
				return true;
			case "diego":
				axolotl.setComponent(DataComponents.AXOLOTL_VARIANT, Axolotl.Variant.WILD);
				return true;
			case "sunshine":
				axolotl.setComponent(DataComponents.AXOLOTL_VARIANT, Axolotl.Variant.GOLD);
				return true;
			case "rivulet":
				axolotl.setComponent(DataComponents.AXOLOTL_VARIANT, Axolotl.Variant.CYAN);
				return true;
			default:
				return false;
		}
	}

	private static boolean setWolfVariant(Wolf wolf, ResourceKey<WolfVariant> key) {
		Registry<WolfVariant> variants = wolf.registryAccess().lookupOrThrow(Registries.WOLF_VARIANT);
		Optional<Holder.Reference<WolfVariant>> variant = variants.get(key);
		if (variant.isEmpty()) {
			return false;
		}

		wolf.setComponent(DataComponents.WOLF_VARIANT, variant.get());
		return true;
	}

	private static boolean setWolfSoundVariant(Wolf wolf, ResourceKey<WolfSoundVariant> key) {
		Registry<WolfSoundVariant> soundVariants = wolf.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT);
		Optional<Holder.Reference<WolfSoundVariant>> soundVariant = soundVariants.get(key);
		if (soundVariant.isEmpty()) {
			return false;
		}

		wolf.setComponent(DataComponents.WOLF_SOUND_VARIANT, soundVariant.get());
		return true;
	}

	private static boolean setCatVariant(Cat cat, ResourceKey<CatVariant> key) {
		Registry<CatVariant> variants = cat.registryAccess().lookupOrThrow(Registries.CAT_VARIANT);
		Optional<Holder.Reference<CatVariant>> variant = variants.get(key);
		if (variant.isEmpty()) {
			return false;
		}

		cat.setComponent(DataComponents.CAT_VARIANT, variant.get());
		return true;
	}
}
