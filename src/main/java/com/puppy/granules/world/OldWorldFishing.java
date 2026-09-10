package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;

public final class OldWorldFishing {
	private OldWorldFishing() {
	}

	public static List<ItemStack> modifyFishingLoot(
		ServerLevel level,
		Vec3 bobberPosition,
		ItemStack rod,
		List<ItemStack> caughtItems,
		LootParams lootParams
	) {
		int galileanLevel = getGalileanLevel(level, rod);
		List<ItemStack> modifiedItems = new ArrayList<>();
		for (ItemStack caughtItem : caughtItems) {
			modifiedItems.add(caughtItem.copy());
		}
		if (!containsFish(modifiedItems) && galileanLevel > 0 && level.getRandom().nextFloat() < galileanLevel * 0.02F) {
			modifiedItems.clear();
			modifiedItems.add(createFishCatch(level));
		}
		boolean inStrongholdRing = isInStrongholdRing(level, bobberPosition);
		for (int index = 0; index < modifiedItems.size(); index++) {
			ItemStack caughtItem = modifiedItems.get(index);
			if (!caughtItem.is(ItemTags.FISHES)) {
				continue;
			}
			if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)
				&& inStrongholdRing
				&& level.getRandom().nextFloat() < 0.01F) {
				caughtItem = new ItemStack(GranulesMod.OLD_WORLD_COD, caughtItem.getCount());
				modifiedItems.set(index, caughtItem);
			}
			if (galileanLevel > 0) {
				caughtItem.grow(galileanLevel);
			}
		}
		if (isOceanOrBeach(level, bobberPosition) && level.getRandom().nextFloat() < 0.01F / (galileanLevel + 1.0F)) {
			modifiedItems.clear();
			modifiedItems.add(createSealedBarrel(level, lootParams));
		}
		return modifiedItems;
	}

	private static boolean isOceanOrBeach(ServerLevel level, Vec3 position) {
		BlockPos pos = BlockPos.containing(position);
		return level.getBiome(pos).is(BiomeTags.IS_OCEAN) || level.getBiome(pos).is(BiomeTags.IS_BEACH);
	}

	private static ItemStack createSealedBarrel(ServerLevel level, LootParams lootParams) {
		List<ItemStack> contents = new ArrayList<>();
		boolean shipwreck = level.getRandom().nextBoolean();
		var lootTableKey = shipwreck ? BuiltInLootTables.SHIPWRECK_SUPPLY : BuiltInLootTables.UNDERWATER_RUIN_BIG;
		contents.addAll(level.getServer().reloadableRegistries().getLootTable(lootTableKey).getRandomItems(lootParams));
		int consumableCount = 1 + level.getRandom().nextInt(2);
		for (int index = 0; index < consumableCount; index++) {
			contents.add(createBonusConsumable(level));
		}
		if (contents.size() > 27) {
			contents = new ArrayList<>(contents.subList(0, 27));
		}
		ItemStack barrel = new ItemStack(Items.BARREL);
		CustomData sealedData = CustomData.EMPTY.update(tag -> tag.putBoolean(SealedBarrelProperties.DATA_KEY, true));
		barrel.set(DataComponents.CUSTOM_DATA, sealedData);
		barrel.set(DataComponents.ITEM_MODEL, SealedBarrelProperties.ITEM_MODEL);
		barrel.set(DataComponents.ITEM_NAME, Component.translatable("block.granules.sealed_barrel"));
		barrel.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
		return barrel;
	}

	private static ItemStack createBonusConsumable(ServerLevel level) {
		int type = level.getRandom().nextInt(3);
		if (type == 2) {
			ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
			Holder<net.minecraft.world.effect.MobEffect> effect = level.getRandom().nextBoolean()
				? MobEffects.WATER_BREATHING
				: MobEffects.NIGHT_VISION;
			stew.set(
				DataComponents.SUSPICIOUS_STEW_EFFECTS,
				new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(effect, 160)))
			);
			return stew;
		}
		List<Holder<Potion>> potions = List.of(
			Potions.WATER_BREATHING,
			Potions.NIGHT_VISION,
			Potions.HEALING,
			Potions.REGENERATION,
			Potions.SWIFTNESS
		);
		Holder<Potion> potion = potions.get(level.getRandom().nextInt(potions.size()));
		return PotionContents.createItemStack(type == 0 ? Items.POTION : Items.SPLASH_POTION, potion);
	}

	private static int getGalileanLevel(ServerLevel level, ItemStack rod) {
		Holder<Enchantment> galilean = level.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.getOrThrow(GranulesMod.GALILEAN);
		return EnchantmentHelper.getItemEnchantmentLevel(galilean, rod);
	}

	private static boolean containsFish(List<ItemStack> items) {
		for (ItemStack item : items) {
			if (item.is(ItemTags.FISHES)) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack createFishCatch(ServerLevel level) {
		int roll = level.getRandom().nextInt(100);
		if (roll < 60) {
			return new ItemStack(Items.COD);
		}
		if (roll < 85) {
			return new ItemStack(Items.SALMON);
		}
		if (roll < 87) {
			return new ItemStack(Items.TROPICAL_FISH);
		}
		return new ItemStack(Items.PUFFERFISH);
	}

	private static boolean isInStrongholdRing(ServerLevel level, Vec3 position) {
		ChunkPos chunkPos = new ChunkPos((int)Math.floor(position.x) >> 4, (int)Math.floor(position.z) >> 4);
		return StrongholdRingLocator.detect(level, chunkPos).isInRing();
	}
}
