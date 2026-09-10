package com.puppy.granules.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

public record PetBedBinding(UUID player, GlobalPos bed) {
	public static final Codec<PetBedBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.CODEC.fieldOf("player").forGetter(PetBedBinding::player),
		GlobalPos.CODEC.fieldOf("bed").forGetter(PetBedBinding::bed)
	).apply(instance, PetBedBinding::new));
}
