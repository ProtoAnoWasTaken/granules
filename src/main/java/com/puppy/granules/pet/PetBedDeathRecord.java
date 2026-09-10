package com.puppy.granules.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

public record PetBedDeathRecord(
	UUID owner,
	UUID pet,
	String name,
	String entityType,
	boolean mount,
	boolean requiresLargeBed,
	CompoundTag entityData
) {
	public static final Codec<PetBedDeathRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("owner").forGetter(PetBedDeathRecord::owner),
		UUIDUtil.CODEC.fieldOf("pet").forGetter(PetBedDeathRecord::pet),
		Codec.STRING.fieldOf("name").forGetter(PetBedDeathRecord::name),
		Codec.STRING.fieldOf("entity_type").forGetter(PetBedDeathRecord::entityType),
		Codec.BOOL.fieldOf("mount").forGetter(PetBedDeathRecord::mount),
		Codec.BOOL.optionalFieldOf("requires_large_bed", false).forGetter(PetBedDeathRecord::requiresLargeBed),
		CompoundTag.CODEC.fieldOf("entity_data").forGetter(PetBedDeathRecord::entityData)
	).apply(instance, PetBedDeathRecord::new));
}
