package com.puppy.granules;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.random.Weighted;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.puppy.granules.block.PhilosopherBlock;
import com.puppy.granules.block.CasterBlock;
import com.puppy.granules.block.JunctionBlock;
import com.puppy.granules.block.MoverBlock;
import com.puppy.granules.block.HoneyLiquidBlock;
import com.puppy.granules.command.DrainCommand;
import com.puppy.granules.block.RedstoneResistorBlock;
import com.puppy.granules.block.PetBedBlock;
import com.puppy.granules.block.PlanterBlock;
import com.puppy.granules.block.PlanterBlockEntity;
import com.puppy.granules.config.MoverConfig;
import com.puppy.granules.config.ContentManifest;
import com.puppy.granules.advancement.GranulesAdvancements;
import com.puppy.granules.entity.FletchersArrowEntity;
import com.puppy.granules.entity.MoverEntity;
import com.puppy.granules.entity.OldWorldCod;
import com.puppy.granules.entity.AmethystDamageTicker;
import com.puppy.granules.fluid.HoneyFluid;
import com.puppy.granules.fletching.FletchingMenu;
import com.puppy.granules.fletching.FletchingRecipe;
import com.puppy.granules.fletching.FletchersArrowItem;
import com.puppy.granules.item.SomnosatchelItem;
import com.puppy.granules.item.EnchantedTomeItem;
import com.puppy.granules.item.EnchantedBlockItem;
import com.puppy.granules.item.BreadCrumbsItem;
import com.puppy.granules.item.BreadHeelsItem;
import com.puppy.granules.item.EnderRadarItem;
import com.puppy.granules.item.PetBedItem;
import com.puppy.granules.block.EnchantedWorkstationBlocks;
import com.puppy.granules.block.EnchantedWorkstationBlockEntity;
import com.puppy.granules.network.BoatJumpHandler;
import com.puppy.granules.network.BoatJumpPayload;
import com.puppy.granules.network.VoidFogPayload;
import com.puppy.granules.world.CryingObsidianDrain;
import com.puppy.granules.world.EnchantedEnchantingTableRecovery;
import com.puppy.granules.world.HoneyCauldronFluidStorage;
import com.puppy.granules.world.VillagerWorkstations;
import com.puppy.granules.world.StrongholdRingPlacement;
import com.puppy.granules.world.BurrowStrongholdStructure;
import com.puppy.granules.world.OldWorldFishing;
import com.puppy.granules.pet.PetBedMenu;
import com.puppy.granules.pet.PetBedService;
import com.puppy.granules.recipe.PetBedRecipe;

public class GranulesMod implements ModInitializer {
	public static final String MOD_ID = "granules";
	public static final TagKey<Fluid> HONEY_TAG = TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath(MOD_ID, "honey"));
	public static final TagKey<Block> MOVER_JUNCTIONS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "mover_junctions"));
	public static final TagKey<Block> MOVER_ADHERENCE_BLOCKS = TagKey.create(
		Registries.BLOCK,
		Identifier.fromNamespaceAndPath(MOD_ID, "mover_adherence_blocks")
	);
	public static final PlacementModifierType<StrongholdRingPlacement> STRONGHOLD_RING_PLACEMENT = registerStrongholdRingPlacement();
	public static final StructureType<BurrowStrongholdStructure> BURROW_STRONGHOLD_STRUCTURE_TYPE = registerBurrowStrongholdStructureType();
	public static final SoundEvent MOVER_MOVING = registerSoundEvent("mover.moving");
	public static final SoundEvent MOVER_MOVING_UNDERWATER = registerSoundEvent("mover.moving_underwater");
	public static final SoundEvent ENDER_RADAR_ENTERED = registerSoundEvent("ender_radar.entered");
	public static final SoundEvent ENDER_RADAR_ACTIVE = registerSoundEvent("ender_radar.active");
	public static final SoundEvent HARD_HAT_BLOCK = registerSoundEvent("hard_hat.block");
	public static final SoundEvent BURROW_MUSIC = registerSoundEvent("music.the_burrow");
	public static final SoundEvent EAT_YOUR_POTATOES_MUSIC = registerSoundEvent("music.eat_your_potatoes");
	public static final SoundEvent WORLD_OF_SYNTHESIS_MUSIC = registerSoundEvent("music.world_of_synthesis");
	public static final SoundEvent FOR_THE_SAKE_OF_MAKING_GAMES_MUSIC = registerSoundEvent("music.for_the_sake_of_making_games");
	public static final SoundEvent VOYAGER_MUSIC = registerSoundEvent("music.voyager");
	public static final SoundEvent LOTUS_MUSIC = registerSoundEvent("music.lotus");
	public static final Holder.Reference<SoundEvent> HARD_HAT_EQUIP = registerSoundEventHolder("hard_hat.equip");
	public static final GameRule<Boolean> VOID_FOG = registerVoidFogGameRule();
	private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> SIMPLE_DUNGEON_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon"));
	private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> STRONGHOLD_CORRIDOR_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/stronghold_corridor"));
	private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> ANCIENT_CITY_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/ancient_city"));
	private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> ABANDONED_MINESHAFT_LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/abandoned_mineshaft"));
	private static final List<Item> BREWED_POTION_CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);
	private static final ResourceKey<PlacedFeature> OLD_WORLD_ROSES_PLACED_FEATURE = ResourceKey.create(
		Registries.PLACED_FEATURE,
		Identifier.fromNamespaceAndPath(MOD_ID, "old_world_roses")
	);
	public static final Item PHANTOM_SCALE = registerItem("phantom_scale");
	public static final Item HARD_HAT = registerHardHat();
	public static final Item ENDER_PEARLET = registerItem("ender_pearlet");
	public static final Item OLD_WORLD_COD = registerOldWorldCodFood("old_world_cod", 4, 0.25F);
	public static final Item COOKED_OLD_WORLD_COD = registerOldWorldCodFood("cooked_old_world_cod", 8, 0.5625F);
	public static final EntityType<OldWorldCod> OLD_WORLD_COD_ENTITY = registerOldWorldCodEntity();
	public static final MobBucketItem OLD_WORLD_COD_BUCKET = registerOldWorldCodBucket();
	public static final SpawnEggItem OLD_WORLD_COD_SPAWN_EGG = registerOldWorldCodSpawnEgg();
	public static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> GALILEAN = ResourceKey.create(
		Registries.ENCHANTMENT,
		Identifier.fromNamespaceAndPath(MOD_ID, "galilean")
	);
	public static final EnderRadarItem ENDER_RADAR = registerEnderRadarItem();
	public static final BreadHeelsItem BREAD_HEELS = registerBreadHeelsItem();
	private static final BreadCrumbsRegistration BREAD_CRUMBS_REGISTRATION = registerBreadCrumbs();
	public static final Block BREAD_CRUMBS_BLOCK = BREAD_CRUMBS_REGISTRATION.block();
	public static final BreadCrumbsItem BREAD_CRUMBS = BREAD_CRUMBS_REGISTRATION.item();
	private static final PetBedRegistration PET_BED_REGISTRATION = registerPetBed();
	public static final PetBedBlock PET_BED = PET_BED_REGISTRATION.block();
	public static final PetBedItem PET_BED_ITEM = PET_BED_REGISTRATION.item();
	private static final PlanterRegistration PLANTER_REGISTRATION = registerPlanter("planter", false);
	public static final PlanterBlock PLANTER = PLANTER_REGISTRATION.block();
	public static final BlockItem PLANTER_ITEM = PLANTER_REGISTRATION.item();
	private static final PlanterRegistration ENCHANTED_PLANTER_REGISTRATION = registerPlanter("enchanted_planter", true);
	public static final PlanterBlock ENCHANTED_PLANTER = ENCHANTED_PLANTER_REGISTRATION.block();
	public static final BlockItem ENCHANTED_PLANTER_ITEM = ENCHANTED_PLANTER_REGISTRATION.item();
	private static final Map<DyeColor, RegisteredBlock> GLOW_WOOL_REGISTRATIONS = registerGlowWoolBlocks();
	private static final RegisteredBlock OLD_WORLD_ROSE_REGISTRATION = registerOldWorldRose("old_world_rose");
	public static final Block OLD_WORLD_ROSE = OLD_WORLD_ROSE_REGISTRATION.block();
	public static final BlockItem OLD_WORLD_ROSE_ITEM = OLD_WORLD_ROSE_REGISTRATION.item();
	private static final RegisteredBlock OLD_WORLD_CYAN_ROSE_REGISTRATION = registerOldWorldRose("old_world_cyan_rose");
	public static final Block OLD_WORLD_CYAN_ROSE = OLD_WORLD_CYAN_ROSE_REGISTRATION.block();
	public static final BlockItem OLD_WORLD_CYAN_ROSE_ITEM = OLD_WORLD_CYAN_ROSE_REGISTRATION.item();
	public static final Block POTTED_OLD_WORLD_ROSE = registerPottedOldWorldRose("potted_old_world_rose", OLD_WORLD_ROSE);
	public static final Block POTTED_OLD_WORLD_CYAN_ROSE = registerPottedOldWorldRose("potted_old_world_cyan_rose", OLD_WORLD_CYAN_ROSE);
	public static final Holder.Reference<Potion> WHEAT_MASTER = registerWheatMasterPotion();
	public static final Holder.Reference<Potion> LONG_WHEAT_MASTER = registerLongWheatMasterPotion();
	public static final Holder.Reference<Potion> STRONG_WHEAT_MASTER = registerStrongWheatMasterPotion();
	public static final Holder.Reference<Potion> BLINDNESS = registerBlindnessPotion();
	public static final Holder.Reference<Potion> LONG_BLINDNESS = registerLongBlindnessPotion();
	public static final Holder.Reference<Potion> STRONG_BLINDNESS = registerStrongBlindnessPotion();
	public static final Holder.Reference<Potion> LONG_LUCK = registerLongLuckPotion();
	public static final Holder.Reference<Potion> STRONG_LUCK = registerStrongLuckPotion();
	public static final Holder.Reference<Potion> BAD_LUCK = registerBadLuckPotion();
	public static final Holder.Reference<Potion> LONG_BAD_LUCK = registerLongBadLuckPotion();
	public static final Holder.Reference<Potion> STRONG_BAD_LUCK = registerStrongBadLuckPotion();
	public static final Holder.Reference<Potion> LOTUS_LOTION = registerLotusLotionPotion();
	public static final Holder.Reference<Potion> LONG_LOTUS_LOTION = registerLongLotusLotionPotion();
	public static final Holder.Reference<Potion> ANCIENT_EYES = registerAncientEyesPotion();
	public static final Holder.Reference<Potion> LONG_ANCIENT_EYES = registerLongAncientEyesPotion();
	public static final Holder.Reference<Potion> STRONG_ANCIENT_EYES = registerStrongAncientEyesPotion();
	public static final Holder.Reference<Potion> ANCIENT_ARMATURE = registerAncientArmaturePotion();
	public static final Holder.Reference<Potion> LONG_ANCIENT_ARMATURE = registerLongAncientArmaturePotion();
	public static final Holder.Reference<Potion> STRONG_ANCIENT_ARMATURE = registerStrongAncientArmaturePotion();
	public static final SomnosatchelItem SOMNOSATCHEL = registerSomnosatchelItem();
	public static final EnchantedTomeItem ENCHANTED_TOME = registerEnchantedTomeItem();
	private static final RegisteredBlock ENCHANTED_ANVIL_REGISTRATION = registerEnchantedAnvil();
	public static final Block ENCHANTED_ANVIL = ENCHANTED_ANVIL_REGISTRATION.block();
	public static final BlockItem ENCHANTED_ANVIL_ITEM = ENCHANTED_ANVIL_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_ENCHANTING_TABLE_REGISTRATION = registerEnchantedEnchantingTable();
	public static final Block ENCHANTED_ENCHANTING_TABLE = ENCHANTED_ENCHANTING_TABLE_REGISTRATION.block();
	public static final BlockItem ENCHANTED_ENCHANTING_TABLE_ITEM = ENCHANTED_ENCHANTING_TABLE_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_SMITHING_TABLE_REGISTRATION = registerEnchantedSmithingTable();
	public static final Block ENCHANTED_SMITHING_TABLE = ENCHANTED_SMITHING_TABLE_REGISTRATION.block();
	public static final BlockItem ENCHANTED_SMITHING_TABLE_ITEM = ENCHANTED_SMITHING_TABLE_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_CRAFTING_TABLE_REGISTRATION = registerEnchantedCraftingTable();
	public static final Block ENCHANTED_CRAFTING_TABLE = ENCHANTED_CRAFTING_TABLE_REGISTRATION.block();
	public static final BlockItem ENCHANTED_CRAFTING_TABLE_ITEM = ENCHANTED_CRAFTING_TABLE_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_FLETCHING_TABLE_REGISTRATION = registerEnchantedFletchingTable();
	public static final Block ENCHANTED_FLETCHING_TABLE = ENCHANTED_FLETCHING_TABLE_REGISTRATION.block();
	public static final BlockItem ENCHANTED_FLETCHING_TABLE_ITEM = ENCHANTED_FLETCHING_TABLE_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_STONECUTTER_REGISTRATION = registerEnchantedStonecutter();
	public static final Block ENCHANTED_STONECUTTER = ENCHANTED_STONECUTTER_REGISTRATION.block();
	public static final BlockItem ENCHANTED_STONECUTTER_ITEM = ENCHANTED_STONECUTTER_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_GRINDSTONE_REGISTRATION = registerEnchantedGrindstone();
	public static final Block ENCHANTED_GRINDSTONE = ENCHANTED_GRINDSTONE_REGISTRATION.block();
	public static final BlockItem ENCHANTED_GRINDSTONE_ITEM = ENCHANTED_GRINDSTONE_REGISTRATION.item();
	private static final RegisteredBlock ENCHANTED_CAULDRON_REGISTRATION = registerEnchantedCauldron();
	public static final Block ENCHANTED_CAULDRON = ENCHANTED_CAULDRON_REGISTRATION.block();
	public static final BlockItem ENCHANTED_CAULDRON_ITEM = ENCHANTED_CAULDRON_REGISTRATION.item();
	public static final BlockEntityType<EnchantedWorkstationBlockEntity> ENCHANTED_WORKSTATION_BLOCK_ENTITY = registerEnchantedWorkstationBlockEntity();
	public static final BlockEntityType<PlanterBlockEntity> PLANTER_BLOCK_ENTITY = registerPlanterBlockEntity();
	private static final RegisteredBlock PHILOSOPHER_REGISTRATION = registerPhilosopherBlock();
	public static final Block PHILOSOPHER = PHILOSOPHER_REGISTRATION.block();
	public static final BlockItem PHILOSOPHER_ITEM = PHILOSOPHER_REGISTRATION.item();
	private static final RegisteredBlock CASTER_REGISTRATION = registerCasterBlock();
	public static final Block CASTER = CASTER_REGISTRATION.block();
	public static final BlockItem CASTER_ITEM = CASTER_REGISTRATION.item();
	private static final RegisteredBlock MOVER_REGISTRATION = registerMoverBlock();
	public static final Block MOVER = MOVER_REGISTRATION.block();
	public static final BlockItem MOVER_ITEM = MOVER_REGISTRATION.item();
	private static final RegisteredBlock JUNCTION_REGISTRATION = registerJunctionBlock();
	public static final Block JUNCTION = JUNCTION_REGISTRATION.block();
	public static final BlockItem JUNCTION_ITEM = JUNCTION_REGISTRATION.item();
	public static final HoneyFluid FLOWING_HONEY = registerFlowingHoneyFluid();
	public static final HoneyFluid HONEY = registerHoneyFluid();
	public static final Block HONEY_FLUID_BLOCK = registerHoneyFluidBlock();
	public static final BucketItem HONEY_BUCKET = registerHoneyBucket();
	private static final RegisteredBlock REDSTONE_RESISTOR_REGISTRATION = registerRedstoneResistorBlock();
	public static final Block REDSTONE_RESISTOR = REDSTONE_RESISTOR_REGISTRATION.block();
	public static final BlockItem REDSTONE_RESISTOR_ITEM = REDSTONE_RESISTOR_REGISTRATION.item();
	public static final FletchersArrowItem FLETCHERS_ARROW = registerFletchersArrowItem();
	public static final EntityType<FletchersArrowEntity> FLETCHERS_ARROW_ENTITY = registerFletchersArrowEntity();
	public static final EntityType<MoverEntity> MOVER_ENTITY = registerMoverEntity();
	public static final Holder.Reference<GameEvent> ECHO_ARROW_SHOOT = Registry.registerForHolder(
		BuiltInRegistries.GAME_EVENT,
		Identifier.fromNamespaceAndPath(MOD_ID, "echo_arrow_shoot"),
		new GameEvent(64)
	);
	public static final MenuType<FletchingMenu> FLETCHING_MENU = Registry.register(
		BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(MOD_ID, "fletching"),
		new MenuType<>(FletchingMenu::new, FeatureFlags.VANILLA_SET)
	);
	public static final MenuType<PetBedMenu> PET_BED_MENU = Registry.register(
		BuiltInRegistries.MENU,
		Identifier.fromNamespaceAndPath(MOD_ID, "pet_bed"),
		new MenuType<>(PetBedMenu::new, FeatureFlags.VANILLA_SET)
	);
	public static final RecipeType<FletchingRecipe> FLETCHING_RECIPE_TYPE = Registry.register(
		BuiltInRegistries.RECIPE_TYPE,
		Identifier.fromNamespaceAndPath(MOD_ID, "fletching"),
		new RecipeType<>() {
			@Override
			public String toString() {
				return "granules:fletching";
			}
		}
	);
	public static final RecipeSerializer<FletchingRecipe> FLETCHING_RECIPE_SERIALIZER = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER,
		Identifier.fromNamespaceAndPath(MOD_ID, "fletching"),
		new RecipeSerializer<>(FletchingRecipe.MAP_CODEC, FletchingRecipe.STREAM_CODEC)
	);
	public static final RecipeSerializer<PetBedRecipe> PET_BED_RECIPE_SERIALIZER = Registry.register(
		BuiltInRegistries.RECIPE_SERIALIZER,
		Identifier.fromNamespaceAndPath(MOD_ID, "pet_bed"),
		new RecipeSerializer<>(PetBedRecipe.MAP_CODEC, PetBedRecipe.STREAM_CODEC)
	);

	@Override
	public void onInitialize() {
		GranulesAdvancements.initialize();
		com.puppy.granules.config.ContentEnabledCondition.initialize();
		com.puppy.granules.enchantment.GranulesEnchantments.initialize();
		com.puppy.granules.disc.DiscContent.initialize();
		com.puppy.granules.bomb.BombContent.initialize();
		com.puppy.granules.rabbit.RabbitContent.initialize();
		com.puppy.granules.rabbit.RabbitHoleContent.initialize();
		MoverConfig.initialize();
		FabricDefaultAttributeRegistry.register(OLD_WORLD_COD_ENTITY, OldWorldCod.createAttributes());
		if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)) {
			registerOldWorldCodSpawns();
		}
		SpawnPlacements.register(
			OLD_WORLD_COD_ENTITY,
			SpawnPlacementTypes.IN_WATER,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			OldWorldCod::checkOldWorldCodSpawnRules
		);
		FabricPotionBrewingBuilder.BUILD.register(builder -> {
			builder.addMix(Potions.AWKWARD, Items.EMERALD_BLOCK, Potions.LUCK);
			builder.addMix(Potions.LUCK, Items.REDSTONE, LONG_LUCK);
			builder.addMix(Potions.LUCK, Items.GLOWSTONE_DUST, STRONG_LUCK);
			builder.addMix(Potions.LUCK, Items.FERMENTED_SPIDER_EYE, BAD_LUCK);
			builder.addMix(LONG_LUCK, Items.FERMENTED_SPIDER_EYE, LONG_BAD_LUCK);
			builder.addMix(STRONG_LUCK, Items.FERMENTED_SPIDER_EYE, STRONG_BAD_LUCK);
			builder.addMix(BAD_LUCK, Items.REDSTONE, LONG_BAD_LUCK);
			builder.addMix(BAD_LUCK, Items.GLOWSTONE_DUST, STRONG_BAD_LUCK);
			builder.addMix(Potions.AWKWARD, Items.POISONOUS_POTATO, WHEAT_MASTER);
			builder.addMix(WHEAT_MASTER, Items.REDSTONE, LONG_WHEAT_MASTER);
			builder.addMix(WHEAT_MASTER, Items.GLOWSTONE_DUST, STRONG_WHEAT_MASTER);
			builder.addMix(Potions.AWKWARD, Items.INK_SAC, BLINDNESS);
			builder.addMix(Potions.INVISIBILITY, Items.FERMENTED_SPIDER_EYE, BLINDNESS);
			builder.addMix(BLINDNESS, Items.REDSTONE, LONG_BLINDNESS);
			builder.addMix(BLINDNESS, Items.GLOWSTONE_DUST, STRONG_BLINDNESS);
			builder.addMix(Potions.AWKWARD, OLD_WORLD_ROSE_ITEM, LOTUS_LOTION);
			builder.addMix(LOTUS_LOTION, Items.REDSTONE, LONG_LOTUS_LOTION);
			builder.addMix(Potions.AWKWARD, OLD_WORLD_CYAN_ROSE_ITEM, LONG_LOTUS_LOTION);
			builder.addMix(Potions.AWKWARD, Items.TORCHFLOWER_SEEDS, ANCIENT_EYES);
			builder.addMix(ANCIENT_EYES, Items.REDSTONE, LONG_ANCIENT_EYES);
			builder.addMix(ANCIENT_EYES, Items.GLOWSTONE_DUST, STRONG_ANCIENT_EYES);
			builder.addMix(Potions.AWKWARD, Items.PITCHER_POD, ANCIENT_ARMATURE);
			builder.addMix(ANCIENT_ARMATURE, Items.REDSTONE, LONG_ANCIENT_ARMATURE);
			builder.addMix(ANCIENT_ARMATURE, Items.GLOWSTONE_DUST, STRONG_ANCIENT_ARMATURE);
		});
		if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)) {
			BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION,
				OLD_WORLD_ROSES_PLACED_FEATURE
			);
		}
		ComposterBlock.COMPOSTABLES.put(OLD_WORLD_ROSE_ITEM, 0.65F);
		ComposterBlock.COMPOSTABLES.put(OLD_WORLD_CYAN_ROSE_ITEM, 0.65F);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> DrainCommand.register(dispatcher, registryAccess));
		PayloadTypeRegistry.serverboundPlay().register(BoatJumpPayload.TYPE, BoatJumpPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VoidFogPayload.TYPE, VoidFogPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(BoatJumpPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> BoatJumpHandler.handle(context.player(), payload));
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sendVoidFogState(handler.player, server.getGameRules().get(VOID_FOG));
		});
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			if (key.equals(SIMPLE_DUNGEON_LOOT_TABLE)) {
				if (!ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
					addEnchantedTomeLoot(tableBuilder, 0.05F);
				}
				addDungeonProduceLoot(tableBuilder, Items.CARROT, 2.0F);
				addDungeonProduceLoot(tableBuilder, Items.POTATO, 1.0F);
				addDungeonProduceLoot(tableBuilder, Items.RED_MUSHROOM, 3.0F);
			}
			if (key.equals(STRONGHOLD_CORRIDOR_LOOT_TABLE)
				&& !ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
				addEnchantedTomeLoot(tableBuilder, 0.1F);
			}
			if (key.equals(ANCIENT_CITY_LOOT_TABLE)
				&& !ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
				addEnchantedTomeLoot(tableBuilder, 0.05F);
			}
            String lootPath = key.identifier().getPath();
            if (lootPath.equals("chests/buried_treasure") || lootPath.startsWith("chests/trial_chambers/")) {
                float enchantmentChance = lootPath.equals("chests/buried_treasure") ? 0.10F : 0.05F;
                addTreasureEnchantmentLoot(tableBuilder, registries, com.puppy.granules.enchantment.GranulesEnchantments.FELLING, enchantmentChance);
                addTreasureEnchantmentLoot(tableBuilder, registries, com.puppy.granules.enchantment.GranulesEnchantments.RADIUS, enchantmentChance);
            }			if (key.equals(ABANDONED_MINESHAFT_LOOT_TABLE)) {
				tableBuilder.withPool(
					LootPool.lootPool()
						.setRolls(UniformGenerator.between(2.0F, 4.0F))
						.add(LootItem.lootTableItem(com.puppy.granules.bomb.BombContent.DYNAMITE).setWeight(10))
						.add(EmptyLootItem.emptyItem().setWeight(88))
						.apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
				);
				tableBuilder.withPool(
					LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1.0F))
						.when(LootItemRandomChanceCondition.randomChance(5.0F / 71.0F))
						.add(
							LootItem.lootTableItem(HARD_HAT)
								.apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0F, 1.0F)))
						)
				);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)) {
				entries.insertBefore(Items.PHANTOM_MEMBRANE, PHANTOM_SCALE);
				entries.insertBefore(Items.ENDER_PEARL, ENDER_PEARLET);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)) {
				entries.insertAfter(Items.POPPY, OLD_WORLD_ROSE_ITEM, OLD_WORLD_CYAN_ROSE_ITEM);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)) {
				entries.insertAfter(Items.DYED_BUNDLE.black(), SOMNOSATCHEL);
			}
			entries.insertAfter(Items.POWDER_SNOW_BUCKET, HONEY_BUCKET);
			if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)) {
				entries.insertAfter(Items.COMPASS, ENDER_RADAR);
				entries.insertAfter(Items.COD_BUCKET, OLD_WORLD_COD_BUCKET);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)) {
				entries.insertAfter(Items.COD_SPAWN_EGG, OLD_WORLD_COD_SPAWN_EGG);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
				entries.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ENCHANTED_TOME);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COLORED_BLOCKS).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.GLOW_WOOL)) {
				entries.insertAfter(getVanillaWoolBlock(DyeColor.PINK), getGlowWoolStacks());
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
			entries.insertAfter(Items.COMPARATOR, REDSTONE_RESISTOR_ITEM);
			if (!ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
				entries.insertAfter(Items.OBSERVER, CASTER_ITEM);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.insertAfter(Blocks.BED.pick(DyeColor.WHITE), PET_BED_ITEM);
			if (!ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
				entries.insertAfter(Items.DECORATED_POT, PLANTER_ITEM, ENCHANTED_PLANTER_ITEM);
				entries.insertAfter(Items.LODESTONE, PHILOSOPHER_ITEM);
				entries.insertAfter(Items.ANVIL, ENCHANTED_ANVIL_ITEM);
				entries.insertAfter(Items.ENCHANTING_TABLE, ENCHANTED_ENCHANTING_TABLE_ITEM);
				entries.insertAfter(Items.SMITHING_TABLE, ENCHANTED_SMITHING_TABLE_ITEM);
				entries.insertAfter(Items.CRAFTING_TABLE, ENCHANTED_CRAFTING_TABLE_ITEM);
				entries.insertAfter(Items.FLETCHING_TABLE, ENCHANTED_FLETCHING_TABLE_ITEM);
				entries.insertAfter(Items.STONECUTTER, ENCHANTED_STONECUTTER_ITEM);
				entries.insertAfter(Items.GRINDSTONE, ENCHANTED_GRINDSTONE_ITEM);
				entries.insertAfter(Items.CAULDRON, ENCHANTED_CAULDRON_ITEM);
			}
			if (!ContentManifest.get().isBanned(ContentManifest.Category.MOVERS)) {
				entries.insertAfter(PHILOSOPHER_ITEM, MOVER_ITEM, JUNCTION_ITEM);
			}
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SEARCH).register(entries -> {
			entries.insertAfter(Blocks.BED.pick(DyeColor.WHITE), PET_BED_ITEM);
			entries.insertAfter(Items.DECORATED_POT, PLANTER_ITEM, ENCHANTED_PLANTER_ITEM);
			entries.insertAfter(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ENCHANTED_TOME);
			entries.insertAfter(Items.OBSERVER, CASTER_ITEM);
			entries.insertAfter(CASTER_ITEM, MOVER_ITEM, JUNCTION_ITEM);
			entries.insertAfter(getVanillaWoolBlock(DyeColor.PINK), getGlowWoolStacks());
			entries.insertAfter(Items.ANVIL, ENCHANTED_ANVIL_ITEM);
			entries.insertAfter(Items.ENCHANTING_TABLE, ENCHANTED_ENCHANTING_TABLE_ITEM);
			entries.insertAfter(Items.SMITHING_TABLE, ENCHANTED_SMITHING_TABLE_ITEM);
			entries.insertAfter(Items.CRAFTING_TABLE, ENCHANTED_CRAFTING_TABLE_ITEM);
			entries.insertAfter(Items.FLETCHING_TABLE, ENCHANTED_FLETCHING_TABLE_ITEM);
			entries.insertAfter(Items.STONECUTTER, ENCHANTED_STONECUTTER_ITEM);
			entries.insertAfter(Items.GRINDSTONE, ENCHANTED_GRINDSTONE_ITEM);
			entries.insertAfter(Items.CAULDRON, ENCHANTED_CAULDRON_ITEM);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.insertAfter(Items.IRON_BOOTS, HARD_HAT);
			if (!ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
				entries.insertAfter(Items.ARROW, FLETCHERS_ARROW);
			}
			movePotionVariantsAfter(entries, List.of(Items.TIPPED_ARROW), Potions.LUCK, List.of(LONG_LUCK, STRONG_LUCK, BAD_LUCK, LONG_BAD_LUCK, STRONG_BAD_LUCK));
			movePotionVariantsAfter(
				entries,
				List.of(Items.TIPPED_ARROW),
				STRONG_BAD_LUCK,
				List.of(ANCIENT_EYES, LONG_ANCIENT_EYES, STRONG_ANCIENT_EYES, ANCIENT_ARMATURE, LONG_ANCIENT_ARMATURE, STRONG_ANCIENT_ARMATURE)
			);
			movePotionVariantsAfter(entries, List.of(Items.TIPPED_ARROW), Potions.REGENERATION, List.of(LOTUS_LOTION, LONG_LOTUS_LOTION));
			movePotionVariantsAfter(
				entries,
				List.of(Items.TIPPED_ARROW),
				Potions.LONG_NIGHT_VISION,
				List.of(BLINDNESS, LONG_BLINDNESS, STRONG_BLINDNESS)
			);
			movePotionVariantsAfter(entries, List.of(Items.TIPPED_ARROW), Potions.TURTLE_MASTER, List.of(WHEAT_MASTER, LONG_WHEAT_MASTER, STRONG_WHEAT_MASTER));
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)) {
				entries.insertAfter(Items.BREAD, BREAD_HEELS, BREAD_CRUMBS);
			}
			if (!ContentManifest.get().isBanned(ContentManifest.Category.OLD_WORLD_ITEMS)) {
				entries.insertAfter(Items.COD, OLD_WORLD_COD);
				entries.insertAfter(Items.COOKED_COD, COOKED_OLD_WORLD_COD);
			}
			movePotionVariantsAfter(entries, BREWED_POTION_CONTAINERS, Potions.LUCK, List.of(LONG_LUCK, STRONG_LUCK, BAD_LUCK, LONG_BAD_LUCK, STRONG_BAD_LUCK));
			movePotionVariantsAfter(
				entries,
				BREWED_POTION_CONTAINERS,
				STRONG_BAD_LUCK,
				List.of(ANCIENT_EYES, LONG_ANCIENT_EYES, STRONG_ANCIENT_EYES, ANCIENT_ARMATURE, LONG_ANCIENT_ARMATURE, STRONG_ANCIENT_ARMATURE)
			);
			movePotionVariantsAfter(entries, BREWED_POTION_CONTAINERS, Potions.REGENERATION, List.of(LOTUS_LOTION, LONG_LOTUS_LOTION));
			movePotionVariantsAfter(
				entries,
				BREWED_POTION_CONTAINERS,
				Potions.LONG_NIGHT_VISION,
				List.of(BLINDNESS, LONG_BLINDNESS, STRONG_BLINDNESS)
			);
			movePotionVariantsAfter(entries, BREWED_POTION_CONTAINERS, Potions.TURTLE_MASTER, List.of(WHEAT_MASTER, LONG_WHEAT_MASTER, STRONG_WHEAT_MASTER));
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SEARCH).register(entries -> {
			entries.insertAfter(Items.IRON_BOOTS, HARD_HAT);
			entries.insertAfter(Items.BREAD, BREAD_HEELS, BREAD_CRUMBS);
			entries.insertAfter(Items.COMPASS, ENDER_RADAR);
			entries.insertAfter(Items.POPPY, OLD_WORLD_ROSE_ITEM, OLD_WORLD_CYAN_ROSE_ITEM);
			entries.insertAfter(Items.COD, OLD_WORLD_COD);
			entries.insertAfter(Items.COOKED_COD, COOKED_OLD_WORLD_COD);
			entries.insertAfter(Items.COD_BUCKET, OLD_WORLD_COD_BUCKET);
			entries.insertAfter(Items.COD_SPAWN_EGG, OLD_WORLD_COD_SPAWN_EGG);
			entries.getDisplayStacks().removeIf(stack -> ContentManifest.get().isBanned(stack.getItem()));
			if (entries.getSearchTabStacks() != entries.getDisplayStacks()) {
				entries.getSearchTabStacks().removeIf(stack -> ContentManifest.get().isBanned(stack.getItem()));
			}
		});
		AmethystDamageTicker.initialize();
		CryingObsidianDrain.initialize();
		EnchantedEnchantingTableRecovery.initialize();
		HoneyCauldronFluidStorage.initialize();
		VillagerWorkstations.initialize();
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			PetBedService.recordDeath((ServerLevel) entity.level(), entity);
			if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)
				&& entity instanceof EnderMan
				&& droppedNoEnderPearl(entity)) {
				entity.spawnAtLocation((ServerLevel) entity.level(), new ItemStack(ENDER_PEARLET));
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, damageSource, baseDamageTaken, damageTaken, blocked) -> {
			if (!ContentManifest.get().isBanned(ContentManifest.Category.LESSER_ITEMS)
				&& entity instanceof Phantom
				&& damageTaken > 0.0F
				&& isEligiblePhantomScaleHit(damageSource)
				&& entity.getRandom().nextBoolean()) {
				entity.spawnAtLocation((ServerLevel) entity.level(), new ItemStack(PHANTOM_SCALE));
			}
		});
		DispenserBlock.registerProjectileBehavior(FLETCHERS_ARROW);
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (isStringArrowWeapon(player.getItemInHand(hand)) && FletchersArrowEntity.hasActiveStringArrow(player)) {
				if (!level.isClientSide()) {
					FletchersArrowEntity.reelStringArrows(player);
				}
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && entity instanceof Sniffer sniffer && player.getItemInHand(hand).is(Items.TORCHFLOWER_SEEDS)) {
				PetBedService.logSniffer(serverPlayer, sniffer);
			}
			return InteractionResult.PASS;
		});
	}

	public static void sendVoidFogState(ServerPlayer player, boolean enabled) {
		if (ServerPlayNetworking.canSend(player, VoidFogPayload.TYPE)) {
			ServerPlayNetworking.send(player, new VoidFogPayload(enabled));
		}
	}

    private static void addTreasureEnchantmentLoot(
        net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
        net.minecraft.core.HolderLookup.Provider registries,
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey,
        float chance
    ) {
        net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment = registries
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(enchantmentKey);
        tableBuilder.withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(
                    LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                        .apply(
                            new net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction.Builder()
                                .withEnchantment(enchantment, ConstantValue.exactly(1.0F))
                        )
                )
        );
    }
	private static boolean isStringArrowWeapon(ItemStack stack) {
		return stack.is(Items.BOW) || stack.is(Items.CROSSBOW);
	}

	private static void addEnchantedTomeLoot(net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder, float chance) {
		tableBuilder.withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.when(LootItemRandomChanceCondition.randomChance(chance))
				.add(LootItem.lootTableItem(ENCHANTED_TOME))
		);
	}

	private static void addDungeonProduceLoot(
		net.minecraft.world.level.storage.loot.LootTable.Builder tableBuilder,
		Item item,
		float maximum
	) {
		tableBuilder.withPool(
			LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.add(
					LootItem.lootTableItem(item)
						.apply(net.minecraft.world.level.storage.loot.functions.SetItemCountFunction.setCount(UniformGenerator.between(0.0F, maximum)))
				)
		);
	}

	private static boolean isEligiblePhantomScaleHit(net.minecraft.world.damagesource.DamageSource damageSource) {
		if (!(damageSource.getEntity() instanceof Player player) || damageSource.getDirectEntity() != player) {
			return false;
		}
		ItemStack weapon = player.getMainHandItem();
		return weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES) || weapon.is(ItemTags.HOES);
	}

	private static boolean droppedNoEnderPearl(net.minecraft.world.entity.LivingEntity entity) {
		return entity.level().getEntitiesOfClass(
			ItemEntity.class,
			entity.getBoundingBox().inflate(1.0D),
			itemEntity -> itemEntity.getItem().is(Items.ENDER_PEARL)
		).isEmpty();
	}

	private static EntityType<FletchersArrowEntity> registerFletchersArrowEntity() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "fletchers_arrow");
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<FletchersArrowEntity> type = EntityType.Builder.<FletchersArrowEntity>of(
			FletchersArrowEntity::new,
			MobCategory.MISC
		)
			.sized(0.5F, 0.5F)
			.clientTrackingRange(4)
			.updateInterval(20)
			.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
	}

	private static EntityType<MoverEntity> registerMoverEntity() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "mover");
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<MoverEntity> type = EntityType.Builder.<MoverEntity>of(MoverEntity::new, MobCategory.MISC)
			.sized(0.98F, 0.98F)
			.clientTrackingRange(8)
			.updateInterval(1)
			.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
	}

	private static EntityType<OldWorldCod> registerOldWorldCodEntity() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "old_world_cod");
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<OldWorldCod> type = EntityType.Builder.<OldWorldCod>of(OldWorldCod::new, MobCategory.WATER_AMBIENT)
			.sized(0.5F, 0.3F)
			.eyeHeight(0.195F)
			.clientTrackingRange(4)
			.build(key);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
	}

	private static Item registerOldWorldCodFood(String path, int nutrition, float saturationModifier) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		Item item = new Item(
			new Item.Properties()
				.setId(key)
				.stacksTo(64)
				.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationModifier).build(), Consumables.defaultFood().build())
		);
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static MobBucketItem registerOldWorldCodBucket() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "old_world_cod_bucket");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		MobBucketItem item = new MobBucketItem(
			OLD_WORLD_COD_ENTITY,
			Fluids.WATER,
			net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH,
			new Item.Properties().setId(key).stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)
		);
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static SpawnEggItem registerOldWorldCodSpawnEgg() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "old_world_cod_spawn_egg");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		SpawnEggItem item = new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(OLD_WORLD_COD_ENTITY));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static void registerOldWorldCodSpawns() {
		BiomeModifications.create(Identifier.fromNamespaceAndPath(MOD_ID, "old_world_cod_spawns"))
			.add(
				ModificationPhase.POST_PROCESSING,
				BiomeSelectors.spawnsOneOf(EntityTypes.COD),
				(selection, context) -> {
					List<Weighted<MobSpawnSettings.SpawnerData>> currentSpawns = List.copyOf(
						context.getMobSpawnSettings().getMobs(MobCategory.WATER_AMBIENT)
					);
					List<Weighted<MobSpawnSettings.SpawnerData>> codSpawns = currentSpawns.stream()
						.filter(spawn -> spawn.value().type() == EntityTypes.COD)
						.toList();
					if (codSpawns.isEmpty()) {
						return;
					}
					context.getMobSpawnSettings().removeSpawns((category, spawn) -> category == MobCategory.WATER_AMBIENT);
					for (Weighted<MobSpawnSettings.SpawnerData> spawn : currentSpawns) {
						context.getMobSpawnSettings().addSpawn(MobCategory.WATER_AMBIENT, spawn.value(), spawn.weight() * 4);
					}
					for (Weighted<MobSpawnSettings.SpawnerData> codSpawn : codSpawns) {
						MobSpawnSettings.SpawnerData spawn = codSpawn.value();
						context.getMobSpawnSettings().addSpawn(
							MobCategory.WATER_AMBIENT,
							new MobSpawnSettings.SpawnerData(OLD_WORLD_COD_ENTITY, spawn.minCount(), spawn.maxCount()),
							codSpawn.weight()
						);
					}
				}
			);
	}

	private static RegisteredBlock registerPhilosopherBlock() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "philosopher");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new PhilosopherBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF)
				.explosionResistance(6.0F)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static RegisteredBlock registerCasterBlock() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "caster");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new CasterBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.OBSERVER)
				.sound(SoundType.STONE)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static RegisteredBlock registerMoverBlock() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "mover");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new MoverBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
				.sound(SoundType.ANVIL)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static RegisteredBlock registerJunctionBlock() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "junction");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new JunctionBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
				.sound(SoundType.STONE)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static Map<DyeColor, RegisteredBlock> registerGlowWoolBlocks() {
		Map<DyeColor, RegisteredBlock> registrations = new EnumMap<>(DyeColor.class);
		for (DyeColor color : DyeColor.values()) {
			registrations.put(color, registerGlowWoolBlock(color));
		}
		return Map.copyOf(registrations);
	}

	private static RegisteredBlock registerGlowWoolBlock(DyeColor color) {
		String path = "glow_" + color.getName() + "_wool";
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new Block(
			BlockBehaviour.Properties.ofFullCopy(Blocks.WOOL.pick(color))
				.lightLevel(state -> 6)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static HoneyFluid registerFlowingHoneyFluid() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "flowing_honey");
		return Registry.register(BuiltInRegistries.FLUID, id, new HoneyFluid.Flowing());
	}

	private static HoneyFluid registerHoneyFluid() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "honey");
		return Registry.register(BuiltInRegistries.FLUID, id, new HoneyFluid.Source());
	}

	private static Block registerHoneyFluidBlock() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "honey_fluid");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new HoneyLiquidBlock(
			HONEY,
			BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE).setId(blockKey)
		);
		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	private static BucketItem registerHoneyBucket() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "honey_bucket");
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BucketItem item = new BucketItem(HONEY, new Item.Properties().setId(itemKey).craftRemainder(Items.BUCKET).stacksTo(1));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static RegisteredBlock registerRedstoneResistorBlock() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "redstone_resistor");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new RedstoneResistorBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.COMPARATOR)
				.destroyTime(0.5F)
				.sound(SoundType.STONE)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static BreadHeelsItem registerBreadHeelsItem() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "bread_heels");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		BreadHeelsItem item = new BreadHeelsItem(
			new Item.Properties()
				.setId(key)
				.stacksTo(64)
				.food(
					new FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).build(),
					Consumables.defaultFood().onConsume(
						new ApplyStatusEffectsConsumeEffect(List.of(
							new MobEffectInstance(MobEffects.SPEED, 300, 0),
							new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 0)
						))
					).build()
				)
		);
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static BreadCrumbsRegistration registerBreadCrumbs() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "bread_crumbs");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new LeafLitterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LEAF_LITTER).setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BreadCrumbsItem item = new BreadCrumbsItem(
			block,
			new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().stacksTo(64).rarity(Rarity.UNCOMMON)
		);
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new BreadCrumbsRegistration(block, item);
	}

	private static PetBedRegistration registerPetBed() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "pet_bed");
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		PetBedBlock block = new PetBedBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)
				.explosionResistance(24.0F)
				.setId(blockKey)
		);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		PetBedItem item = new PetBedItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().stacksTo(64));
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new PetBedRegistration(block, item);
	}

	private static PlanterRegistration registerPlanter(String path, boolean enchanted) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		PlanterBlock block = new PlanterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DECORATED_POT).setId(blockKey), enchanted);
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = enchanted
			? new EnchantedBlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().rarity(Rarity.RARE))
			: new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new PlanterRegistration(block, item);
	}

	private static FletchersArrowItem registerFletchersArrowItem() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "fletchers_arrow");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		FletchersArrowItem item = new FletchersArrowItem(new Item.Properties().setId(key).stacksTo(64));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static EnderRadarItem registerEnderRadarItem() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "ender_radar");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		EnderRadarItem item = new EnderRadarItem(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.UNCOMMON));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static SomnosatchelItem registerSomnosatchelItem() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "somnosatchel");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		SomnosatchelItem item = new SomnosatchelItem(
			new Item.Properties().setId(key).stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)
		);
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static EnchantedTomeItem registerEnchantedTomeItem() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "enchanted_tome");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		EnchantedTomeItem item = new EnchantedTomeItem(new Item.Properties().setId(key).stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static RegisteredBlock registerEnchantedAnvil() {
		return registerBlock("enchanted_anvil", properties -> new EnchantedWorkstationBlocks.EnchantedAnvil(properties), Blocks.ANVIL);
	}

	private static RegisteredBlock registerEnchantedEnchantingTable() {
		return registerBlock("enchanted_enchanting_table", properties -> new EnchantedWorkstationBlocks.EnchantedEnchantingTable(properties), Blocks.ENCHANTING_TABLE);
	}

	private static RegisteredBlock registerEnchantedSmithingTable() {
		return registerBlock("enchanted_smithing_table", properties -> new EnchantedWorkstationBlocks.EnchantedSmithingTable(properties), Blocks.SMITHING_TABLE);
	}

	private static RegisteredBlock registerEnchantedCraftingTable() {
		return registerBlock("enchanted_crafting_table", properties -> new EnchantedWorkstationBlocks.EnchantedCraftingTable(properties), Blocks.CRAFTING_TABLE);
	}

	private static RegisteredBlock registerEnchantedFletchingTable() {
		return registerBlock("enchanted_fletching_table", properties -> new EnchantedWorkstationBlocks.EnchantedFletchingTable(properties), Blocks.FLETCHING_TABLE);
	}

	private static RegisteredBlock registerEnchantedStonecutter() {
		return registerBlock("enchanted_stonecutter", properties -> new EnchantedWorkstationBlocks.EnchantedStonecutter(properties), Blocks.STONECUTTER);
	}

	private static RegisteredBlock registerEnchantedGrindstone() {
		return registerBlock("enchanted_grindstone", properties -> new EnchantedWorkstationBlocks.EnchantedGrindstone(properties), Blocks.GRINDSTONE);
	}

	private static RegisteredBlock registerEnchantedCauldron() {
		return registerBlock("enchanted_cauldron", properties -> new EnchantedWorkstationBlocks.EnchantedCauldron(properties), Blocks.CAULDRON);
	}

	private static BlockEntityType<EnchantedWorkstationBlockEntity> registerEnchantedWorkstationBlockEntity() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "enchanted_workstation");
		BlockEntityType<EnchantedWorkstationBlockEntity> type = new BlockEntityType<>(
			EnchantedWorkstationBlockEntity::new,
			Set.of(
				ENCHANTED_ANVIL,
				ENCHANTED_SMITHING_TABLE,
				ENCHANTED_CRAFTING_TABLE,
				ENCHANTED_FLETCHING_TABLE,
				ENCHANTED_STONECUTTER,
				ENCHANTED_GRINDSTONE,
				ENCHANTED_CAULDRON
			)
		);
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
	}

	private static BlockEntityType<PlanterBlockEntity> registerPlanterBlockEntity() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "planter");
		BlockEntityType<PlanterBlockEntity> type = new BlockEntityType<>(
			PlanterBlockEntity::new,
			Set.of(PLANTER, ENCHANTED_PLANTER)
		);
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
	}

	private static RegisteredBlock registerBlock(String path, Function<BlockBehaviour.Properties, Block> factory, Block template) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(template).setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new EnchantedBlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().rarity(net.minecraft.world.item.Rarity.RARE));
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static RegisteredBlock registerOldWorldRose(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).setId(blockKey));
		Registry.register(BuiltInRegistries.BLOCK, id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).stacksTo(64).useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON));
		Registry.register(BuiltInRegistries.ITEM, id, item);
		item.registerBlocks(Item.BY_BLOCK, item);
		return new RegisteredBlock(block, item);
	}

	private static Block registerPottedOldWorldRose(String path, Block flower) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
		Block block = new FlowerPotBlock(flower, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY).setId(key));
		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	private static GameRule<Boolean> registerVoidFogGameRule() {
		GameRule<Boolean> rule = new GameRule<>(
			GameRuleCategory.MISC,
			GameRuleType.BOOL,
			BoolArgumentType.bool(),
			GameRuleTypeVisitor::visitBoolean,
			Codec.BOOL,
			value -> value ? 1 : 0,
			true,
			FeatureFlagSet.of()
		);
		return Registry.register(BuiltInRegistries.GAME_RULE, Identifier.withDefaultNamespace("void_fog"), rule);
	}

	private static SoundEvent registerSoundEvent(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private static Holder.Reference<SoundEvent> registerSoundEventHolder(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private static PlacementModifierType<StrongholdRingPlacement> registerStrongholdRingPlacement() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "stronghold_ring");
		return Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, id, () -> StrongholdRingPlacement.CODEC);
	}

	private static StructureType<BurrowStrongholdStructure> registerBurrowStrongholdStructureType() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "burrow_stronghold");
		return Registry.register(BuiltInRegistries.STRUCTURE_TYPE, id, () -> BurrowStrongholdStructure.CODEC);
	}

	private static Item registerItem(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		Item item = new Item(new Item.Properties().setId(key).stacksTo(64));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	private static Item registerHardHat() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "hard_hat");
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, id);
		ArmorMaterial iron = ArmorMaterials.IRON;
		ArmorMaterial material = new ArmorMaterial(
			iron.durability(),
			iron.defense(),
			iron.enchantmentValue(),
			HARD_HAT_EQUIP,
			iron.toughness(),
			iron.knockbackResistance(),
			iron.repairIngredient(),
			asset
		);
		Item item = new Item(new Item.Properties().setId(key).humanoidArmor(material, ArmorType.HELMET));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}

	public static Block getGlowWoolBlock(DyeColor color) {
		return GLOW_WOOL_REGISTRATIONS.get(color).block();
	}

	public static BlockItem getGlowWoolItem(DyeColor color) {
		return GLOW_WOOL_REGISTRATIONS.get(color).item();
	}

	private static List<ItemStack> getGlowWoolStacks() {
		List<ItemStack> glowWoolStacks = new ArrayList<>();
		for (DyeColor color : DyeColor.values()) {
			glowWoolStacks.add(new ItemStack(getGlowWoolItem(color)));
		}
		return glowWoolStacks;
	}

	public static Block getVanillaWoolBlock(DyeColor color) {
		return Blocks.WOOL.pick(color);
	}

	public static Block getVanillaWoolBlock(Block glowWoolBlock) {
		for (DyeColor color : DyeColor.values()) {
			if (getGlowWoolBlock(color) == glowWoolBlock) {
				return getVanillaWoolBlock(color);
			}
		}
		return null;
	}

	private static Holder.Reference<Potion> registerWheatMasterPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "wheat_master");
		Potion potion = new Potion(
			"wheat_master",
			new MobEffectInstance(MobEffects.HASTE, 400, 3),
			new MobEffectInstance(MobEffects.HUNGER, 400, 2)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongWheatMasterPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_wheat_master");
		Potion potion = new Potion(
			"wheat_master",
			new MobEffectInstance(MobEffects.HASTE, 800, 3),
			new MobEffectInstance(MobEffects.HUNGER, 800, 2)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerStrongWheatMasterPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "strong_wheat_master");
		Potion potion = new Potion(
			"wheat_master",
			new MobEffectInstance(MobEffects.HASTE, 400, 5),
			new MobEffectInstance(MobEffects.HUNGER, 400, 4)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerBlindnessPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "blindness");
		Potion potion = new Potion(
			"blindness",
			new MobEffectInstance(MobEffects.BLINDNESS, 400, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongBlindnessPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_blindness");
		Potion potion = new Potion(
			"blindness",
			new MobEffectInstance(MobEffects.BLINDNESS, 800, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerStrongBlindnessPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "strong_blindness");
		Potion potion = new Potion(
			"blindness",
			new MobEffectInstance(MobEffects.BLINDNESS, 400, 1)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongLuckPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_luck");
		Potion potion = new Potion(
			"luck",
			new MobEffectInstance(MobEffects.LUCK, 12000, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerStrongLuckPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "strong_luck");
		Potion potion = new Potion(
			"luck",
			new MobEffectInstance(MobEffects.LUCK, 6000, 1)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerBadLuckPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "bad_luck");
		Potion potion = new Potion(
			"bad_luck",
			new MobEffectInstance(MobEffects.UNLUCK, 6000, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongBadLuckPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_bad_luck");
		Potion potion = new Potion(
			"bad_luck",
			new MobEffectInstance(MobEffects.UNLUCK, 12000, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerStrongBadLuckPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "strong_bad_luck");
		Potion potion = new Potion(
			"bad_luck",
			new MobEffectInstance(MobEffects.UNLUCK, 6000, 1)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLotusLotionPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "lotus_lotion");
		Potion potion = new Potion(
			"lotus_lotion",
			new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0),
			new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongLotusLotionPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_lotus_lotion");
		Potion potion = new Potion(
			"lotus_lotion",
			new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0),
			new MobEffectInstance(MobEffects.RESISTANCE, 2400, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerAncientEyesPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "ancient_eyes");
		Potion potion = new Potion(
			"ancient_eyes",
			new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0),
			new MobEffectInstance(MobEffects.GLOWING, 1200, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongAncientEyesPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_ancient_eyes");
		Potion potion = new Potion(
			"ancient_eyes",
			new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0),
			new MobEffectInstance(MobEffects.GLOWING, 2400, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerStrongAncientEyesPotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "strong_ancient_eyes");
		Potion potion = new Potion(
			"ancient_eyes",
			new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0),
			new MobEffectInstance(MobEffects.GLOWING, 1200, 1)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerAncientArmaturePotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "ancient_armature");
		Potion potion = new Potion(
			"ancient_armature",
			new MobEffectInstance(MobEffects.ABSORPTION, 3600, 1),
			new MobEffectInstance(MobEffects.SATURATION, 1200, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerLongAncientArmaturePotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "long_ancient_armature");
		Potion potion = new Potion(
			"ancient_armature",
			new MobEffectInstance(MobEffects.ABSORPTION, 7200, 1),
			new MobEffectInstance(MobEffects.SATURATION, 2400, 0)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static Holder.Reference<Potion> registerStrongAncientArmaturePotion() {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "strong_ancient_armature");
		Potion potion = new Potion(
			"ancient_armature",
			new MobEffectInstance(MobEffects.ABSORPTION, 3600, 2),
			new MobEffectInstance(MobEffects.SATURATION, 1200, 1)
		);
		return Registry.registerForHolder(BuiltInRegistries.POTION, id, potion);
	}

	private static void movePotionVariantsAfter(
		FabricCreativeModeTabOutput entries,
		List<Item> containers,
		Holder<Potion> referencePotion,
		List<Holder<Potion>> potions
	) {
		List<ItemStack> displayStacks = entries.getDisplayStacks();
		movePotionVariantsAfter(displayStacks, containers, referencePotion, potions);
		List<ItemStack> searchStacks = entries.getSearchTabStacks();
		if (searchStacks != displayStacks) {
			movePotionVariantsAfter(searchStacks, containers, referencePotion, potions);
		}
	}

	private static void movePotionVariantsAfter(
		List<ItemStack> stacks,
		List<Item> containers,
		Holder<Potion> referencePotion,
		List<Holder<Potion>> potions
	) {
		for (Item container : containers) {
			List<ItemStack> potionStacks = new ArrayList<>();
			for (Holder<Potion> potion : potions) {
				int potionIndex = findPotionStackIndex(stacks, container, potion);
				if (potionIndex >= 0) {
					potionStacks.add(stacks.remove(potionIndex));
				}
			}
			int referenceIndex = findPotionStackIndex(stacks, container, referencePotion);
			if (referenceIndex >= 0) {
				stacks.addAll(referenceIndex + 1, potionStacks);
			}
		}
	}

	private static int findPotionStackIndex(List<ItemStack> stacks, Item container, Holder<Potion> potion) {
		ItemStack expectedStack = PotionContents.createItemStack(container, potion);
		for (int index = 0; index < stacks.size(); index++) {
			if (ItemStack.isSameItemSameComponents(stacks.get(index), expectedStack)) {
				return index;
			}
		}
		return -1;
	}

	private record RegisteredBlock(Block block, BlockItem item) {
	}

	private record BreadCrumbsRegistration(Block block, BreadCrumbsItem item) {
	}

	private record PetBedRegistration(PetBedBlock block, PetBedItem item) {
	}

	private record PlanterRegistration(PlanterBlock block, BlockItem item) {
	}
}



