package com.puppy.granules.world;

import com.puppy.granules.GranulesMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class HardHatProtection {
	private HardHatProtection() {
	}

	public static boolean protects(Player player, AABB fallingBounds) {
		if (!player.getItemBySlot(EquipmentSlot.HEAD).is(GranulesMod.HARD_HAT)) {
			return false;
		}
		AABB playerBounds = player.getBoundingBox();
		AABB headBounds = new AABB(
			playerBounds.minX,
			playerBounds.maxY - 0.45D,
			playerBounds.minZ,
			playerBounds.maxX,
			playerBounds.maxY + 0.2D,
			playerBounds.maxZ
		);
		return fallingBounds.intersects(headBounds);
	}

	public static void absorbImpact(ServerLevel level, Player player) {
		level.playSound(
			null,
			player.getX(),
			player.getY(),
			player.getZ(),
			GranulesMod.HARD_HAT_BLOCK,
			SoundSource.PLAYERS,
			1.0F,
			1.0F
		);
		if (level.getRandom().nextBoolean()) {
			player.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(1, player, EquipmentSlot.HEAD);
		}
	}
}
