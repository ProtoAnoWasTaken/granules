package com.puppy.granules.pet;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.block.PetBedBlock;
import com.puppy.granules.rabbit.KillerRabbitAccess;
import com.puppy.granules.rabbit.RabbitContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public final class PetBedService {
	private static final int RESURRECTION_LEVEL_COST = 5;
	private static final String PET_OWNER_TAG_PREFIX = "granules:pet_bed_owner=";
	private static final String PET_DELISTED_TAG_PREFIX = "granules:pet_bed_delisted=";

	private PetBedService() {
	}

	public static void logSniffer(ServerPlayer player, Sniffer sniffer) {
		if (ownerOf(sniffer) == null) {
			sniffer.addTag(PET_OWNER_TAG_PREFIX + player.getUUID());
		}
	}

	public static void recordDeath(ServerLevel level, LivingEntity entity) {
		UUID owner = ownerOf(entity);
		if (owner == null || !isPet(entity)) {
			return;
		}
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		entity.save(output);
		CompoundTag entityData = output.buildResult();
		scrubDroppedItemData(entityData);
		PetBedSavedData.get(level.getServer()).recordDeath(new PetBedDeathRecord(
			owner,
			entity.getUUID(),
			entity.getName().getString(),
			EntityType.getKey(entity.getType()).toString(),
			entity instanceof AbstractHorse,
			requiresLargeBed(entity),
			entityData
		));
	}

	public static List<PetEntry> entriesFor(ServerPlayer player) {
		Map<UUID, PetEntry> livePets = new LinkedHashMap<>();
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity livingEntity && isAvailableTo(livingEntity, player.getUUID())) {
					boolean mount = livingEntity instanceof AbstractHorse;
					livePets.put(livingEntity.getUUID(), new PetEntry(
						livingEntity.getUUID(),
						livingEntity.getName().getString(),
						livingEntity.getType(),
						false,
						mount,
						true,
						livingEntity instanceof KillerRabbitAccess access && access.granules$isTamedKillerRabbit()
					));
				}
			}
		}
		List<PetEntry> entries = new ArrayList<>(livePets.values());
		for (PetBedDeathRecord record : PetBedSavedData.get(player.level().getServer()).deadPetsFor(player.getUUID())) {
			if (!livePets.containsKey(record.pet())) {
				entries.add(new PetEntry(record.pet(), record.name(), null, true, record.mount(), false, false));
			}
		}
		entries.sort(Comparator.comparing(PetEntry::dead).thenComparing(PetEntry::name, String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(entries);
	}

	public static ItemStack iconFor(PetEntry entry) {
		Item icon;
		if (entry.dead()) {
			icon = Items.TOTEM_OF_UNDYING;
		} else if (entry.killerRabbit()) {
			icon = RabbitContent.KILLER_BUNNY_SPAWN_EGG;
		} else {
			icon = SpawnEggItem.byId(entry.type()).map(Holder::value).orElse(Items.NAME_TAG);
		}
		ItemStack stack = new ItemStack(icon);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(entry.name()));
		Component status = entry.dead()
			? Component.translatable("block.granules.pet_bed.status.dead", RESURRECTION_LEVEL_COST)
			: entry.mount()
				? Component.translatable("block.granules.pet_bed.status.mount")
			: entry.summonable()
				? Component.translatable("block.granules.pet_bed.status.sitting")
				: Component.translatable("block.granules.pet_bed.status.awake");
		Component leftAction = Component.translatable("block.granules.pet_bed.action.left")
			.withStyle(ChatFormatting.BLUE)
			.append(
				Component.translatable(
					entry.dead()
						? "block.granules.pet_bed.action.resurrect"
						: "block.granules.pet_bed.action.summon"
				).withStyle(ChatFormatting.WHITE)
			);
		Component rightAction = Component.translatable("block.granules.pet_bed.action.right")
			.withStyle(ChatFormatting.BLUE)
			.append(Component.translatable("block.granules.pet_bed.action.delist").withStyle(ChatFormatting.WHITE))
			.append(Component.translatable("block.granules.pet_bed.action.confirm").withStyle(ChatFormatting.GRAY));
		stack.set(DataComponents.LORE, new ItemLore(List.of(status, Component.empty(), leftAction, rightAction)));
		return stack;
	}

	public static void delist(ServerPlayer player, PetEntry entry) {
		if (entry.dead()) {
			PetBedSavedData.get(player.level().getServer()).removeDeath(player.getUUID(), entry.pet());
		} else {
			LivingEntity pet = findLivePet(player, entry.pet());
			if (pet == null) {
				return;
			}
			pet.addTag(PET_DELISTED_TAG_PREFIX + player.getUUID());
		}
		player.sendSystemMessage(Component.translatable("block.granules.pet_bed.delisted", entry.name()), true);
	}

	public static void activate(ServerPlayer player, ServerLevel level, BlockPos bedPos, PetEntry entry) {
		if (entry.dead()) {
			resurrect(player, level, bedPos, entry.pet());
			return;
		}
		LivingEntity pet = findLivePet(player, entry.pet());
		if (pet == null) {
			return;
		}
		claimNamedPet(player, pet);
		ServerLevel originLevel = (ServerLevel) pet.level();
		double originX = pet.getX();
		double originY = pet.getY();
		double originZ = pet.getZ();
		double destinationX = bedPos.getX() + 0.5D;
		double destinationY = bedPos.getY() + 0.125D;
		double destinationZ = bedPos.getZ() + 0.5D;
		if (!pet.teleportTo(level, destinationX, destinationY, destinationZ, Set.of(), pet.getYRot(), pet.getXRot(), false)) {
			return;
		}
		originLevel.sendParticles(ParticleTypes.PORTAL, originX, originY + 0.5D, originZ, 24, 0.3D, 0.5D, 0.3D, 0.08D);
		level.sendParticles(ParticleTypes.PORTAL, destinationX, destinationY + 0.5D, destinationZ, 24, 0.3D, 0.5D, 0.3D, 0.08D);
		originLevel.playSound(null, originX, originY, originZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.playSound(null, bedPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private static void resurrect(ServerPlayer player, ServerLevel level, BlockPos bedPos, UUID petId) {
		PetBedSavedData data = PetBedSavedData.get(player.level().getServer());
		PetBedDeathRecord record = data.findDeadPet(player.getUUID(), petId).orElse(null);
		if (record == null) {
			return;
		}
		BlockState state = level.getBlockState(bedPos);
		if (!state.is(GranulesMod.PET_BED)) {
			return;
		}
		if ((record.mount() || record.requiresLargeBed()) && !hasMountBedLayout(level, bedPos)) {
			player.sendSystemMessage(Component.translatable("block.granules.pet_bed.mount_layout"), true);
			return;
		}
		if ("minecraft:nautilus".equals(record.entityType()) && !state.getValue(PetBedBlock.WATERLOGGED)) {
			player.sendSystemMessage(Component.translatable("block.granules.pet_bed.nautilus_water"), true);
			return;
		}
		if (player.experienceLevel < RESURRECTION_LEVEL_COST) {
			player.sendSystemMessage(Component.translatable("block.granules.pet_bed.not_enough_levels", RESURRECTION_LEVEL_COST), true);
			return;
		}
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), record.entityData().copy());
		Entity loaded = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.MOB_SUMMONED, entity -> entity);
		if (!(loaded instanceof LivingEntity revived)) {
			return;
		}
		revived.setPos(bedPos.getX() + 0.5D, bedPos.getY() + 0.125D, bedPos.getZ() + 0.5D);
		revived.setHealth(revived.getMaxHealth());
		if (!level.addFreshEntity(revived)) {
			return;
		}
		player.giveExperienceLevels(-RESURRECTION_LEVEL_COST);
		data.removeDeath(player.getUUID(), petId);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, revived.getX(), revived.getY() + 0.6D, revived.getZ(), 48, 0.4D, 0.6D, 0.4D, 0.1D);
		level.playSound(null, revived.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
	}

	private static LivingEntity findLivePet(ServerPlayer player, UUID petId) {
		for (ServerLevel level : player.level().getServer().getAllLevels()) {
			Entity entity = level.getEntity(petId);
			if (entity instanceof LivingEntity livingEntity && isAvailableTo(livingEntity, player.getUUID())) {
				return livingEntity;
			}
		}
		return null;
	}

	private static boolean hasMountBedLayout(ServerLevel level, BlockPos pos) {
		return mountBedLayoutOrigin(level, pos) != null;
	}

	private static BlockPos mountBedLayoutOrigin(ServerLevel level, BlockPos pos) {
		for (int xOffset : List.of(0, -1)) {
			for (int zOffset : List.of(0, -1)) {
				BlockPos northWest = pos.offset(xOffset, 0, zOffset);
				if (
					hasFacing(level, northWest, net.minecraft.core.Direction.SOUTH)
						&& hasFacing(level, northWest.east(), net.minecraft.core.Direction.WEST)
						&& hasFacing(level, northWest.south(), net.minecraft.core.Direction.EAST)
						&& hasFacing(level, northWest.east().south(), net.minecraft.core.Direction.NORTH)
				) {
					return northWest;
				}
			}
		}
		return null;
	}

	private static boolean hasFacing(ServerLevel level, BlockPos pos, net.minecraft.core.Direction facing) {
		BlockState state = level.getBlockState(pos);
		return state.is(GranulesMod.PET_BED) && state.getValue(PetBedBlock.FACING) == facing;
	}

	private static boolean isOwnedBy(LivingEntity entity, UUID owner) {
		return isPet(entity) && owner.equals(ownerOf(entity));
	}

	private static boolean isAvailableTo(LivingEntity entity, UUID owner) {
		if (entity.entityTags().contains(PET_DELISTED_TAG_PREFIX + owner)) {
			return false;
		}
		UUID existingOwner = ownerOf(entity);
		if (existingOwner != null) {
			return owner.equals(existingOwner);
		}
		return entity.hasCustomName();
	}

	private static void claimNamedPet(ServerPlayer player, LivingEntity entity) {
		if (ownerOf(entity) == null && entity.hasCustomName()) {
			entity.addTag(PET_OWNER_TAG_PREFIX + player.getUUID());
		}
	}

	private static boolean isPet(LivingEntity entity) {
		if (entity instanceof KillerRabbitAccess access && access.granules$isTamedKillerRabbit()) {
			return true;
		}
		if (entity instanceof TamableAnimal tamableAnimal) {
			return tamableAnimal.isTame();
		}
		if (entity instanceof AbstractHorse horse) {
			return horse.isTamed();
		}
		return entity instanceof OwnableEntity || ownerOf(entity) != null;
	}

	private static boolean requiresLargeBed(LivingEntity entity) {
		return entity instanceof AbstractHorse
			|| entity instanceof Sniffer
			|| entity.getBbWidth() > 1.25F
			|| entity.getBbHeight() > 1.5F;
	}

	private static UUID ownerOf(LivingEntity entity) {
		if (entity instanceof KillerRabbitAccess access && access.granules$ownerUuid() != null) {
			return access.granules$ownerUuid();
		}
		if (entity instanceof OwnableEntity ownableEntity) {
			EntityReference<LivingEntity> ownerReference = ownableEntity.getOwnerReference();
			if (ownerReference != null) {
				return ownerReference.getUUID();
			}
		}
		for (String tag : entity.entityTags()) {
			if (!tag.startsWith(PET_OWNER_TAG_PREFIX)) {
				continue;
			}
			try {
				return UUID.fromString(tag.substring(PET_OWNER_TAG_PREFIX.length()));
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}
		return null;
	}

	private static void scrubDroppedItemData(CompoundTag entityData) {
		entityData.remove("Items");
		entityData.remove("ArmorItems");
		entityData.remove("HandItems");
		entityData.remove("SaddleItem");
		entityData.remove("ArmorItem");
		entityData.remove("equipment");
		entityData.remove("inventory");
	}

	public record PetEntry(
		UUID pet,
		String name,
		EntityType<?> type,
		boolean dead,
		boolean mount,
		boolean summonable,
		boolean killerRabbit
	) {
	}
}
