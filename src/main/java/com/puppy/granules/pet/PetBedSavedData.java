package com.puppy.granules.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.puppy.granules.GranulesMod;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PetBedSavedData extends SavedData {
	private static final Codec<PetBedSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		PetBedDeathRecord.CODEC.listOf().optionalFieldOf("dead_pets", List.of()).forGetter(PetBedSavedData::deadPets),
		PetBedBinding.CODEC.listOf().optionalFieldOf("bindings", List.of()).forGetter(PetBedSavedData::bindings)
	).apply(instance, PetBedSavedData::new));
	private static final SavedDataType<PetBedSavedData> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "pet_beds"),
		PetBedSavedData::new,
		CODEC,
		DataFixTypes.LEVEL
	);
	private final List<PetBedDeathRecord> deadPets;
	private final List<PetBedBinding> bindings;

	private PetBedSavedData() {
		this(List.of(), List.of());
	}

	private PetBedSavedData(List<PetBedDeathRecord> deadPets, List<PetBedBinding> bindings) {
		this.deadPets = new ArrayList<>(deadPets);
		this.bindings = new ArrayList<>(bindings);
	}

	public static PetBedSavedData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public List<PetBedDeathRecord> deadPetsFor(UUID owner) {
		return deadPets.stream()
			.filter(record -> record.owner().equals(owner))
			.map(this::copyRecord)
			.toList();
	}

	private List<PetBedDeathRecord> deadPets() {
		return deadPets;
	}

	private List<PetBedBinding> bindings() {
		return bindings;
	}

	public Optional<PetBedDeathRecord> findDeadPet(UUID owner, UUID pet) {
		return deadPets.stream()
			.filter(record -> record.owner().equals(owner) && record.pet().equals(pet))
			.findFirst()
			.map(this::copyRecord);
	}

	public void recordDeath(PetBedDeathRecord deathRecord) {
		deadPets.removeIf(record -> record.owner().equals(deathRecord.owner()) && record.pet().equals(deathRecord.pet()));
		deadPets.add(copyRecord(deathRecord));
		setDirty();
	}

	public void removeDeath(UUID owner, UUID pet) {
		if (deadPets.removeIf(record -> record.owner().equals(owner) && record.pet().equals(pet))) {
			setDirty();
		}
	}

	public boolean bind(UUID player, GlobalPos bed) {
		if (isBoundTo(player, bed)) {
			return false;
		}
		bindings.removeIf(binding -> binding.player().equals(player));
		bindings.add(new PetBedBinding(player, bed));
		setDirty();
		return true;
	}

	public boolean isBoundTo(UUID player, GlobalPos bed) {
		return bindings.stream().anyMatch(binding -> binding.player().equals(player) && binding.bed().equals(bed));
	}

	public Optional<GlobalPos> boundBed(UUID player) {
		return bindings.stream()
			.filter(binding -> binding.player().equals(player))
			.map(PetBedBinding::bed)
			.findFirst();
	}

	private PetBedDeathRecord copyRecord(PetBedDeathRecord record) {
		return new PetBedDeathRecord(
			record.owner(),
			record.pet(),
			record.name(),
			record.entityType(),
			record.mount(),
			record.requiresLargeBed(),
			record.entityData().copy()
		);
	}
}
