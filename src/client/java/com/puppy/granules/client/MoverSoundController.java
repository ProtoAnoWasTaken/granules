package com.puppy.granules.client;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.entity.MoverEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class MoverSoundController {
	private static final Map<Integer, MoverSoundInstance> SOUNDS = new HashMap<>();

	private MoverSoundController() {
	}

	public static void tick(Minecraft client) {
		if (client.level == null) {
			stopAll();
			return;
		}
		Set<Integer> movingIds = new HashSet<>();
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof MoverEntity mover) || mover.getMoverSpeed() <= 0.0F) {
				continue;
			}
			int id = mover.getId();
			movingIds.add(id);
			boolean underwater = mover.isMovingThroughFluid();
			MoverSoundInstance existing = SOUNDS.get(id);
			if (existing != null && existing.isUnderwater() != underwater) {
				existing.end();
				SOUNDS.remove(id);
				existing = null;
			}
			if (existing == null || existing.isStopped()) {
				MoverSoundInstance sound = new MoverSoundInstance(
					mover,
					underwater ? GranulesMod.MOVER_MOVING_UNDERWATER : GranulesMod.MOVER_MOVING,
					underwater
				);
				SOUNDS.put(id, sound);
				client.getSoundManager().play(sound);
			}
		}
		SOUNDS.entrySet().removeIf(entry -> {
			if (movingIds.contains(entry.getKey())) {
				return false;
			}
			entry.getValue().end();
			return true;
		});
	}

	private static void stopAll() {
		for (MoverSoundInstance sound : SOUNDS.values()) {
			sound.end();
		}
		SOUNDS.clear();
	}
}
