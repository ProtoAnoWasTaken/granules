package com.protoano.granules.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class Necrosoles {
    private Necrosoles() {
    }

    public static boolean active(LivingEntity entity) {
        return entity instanceof Player player && !player.isSpectator() && !player.getAbilities().flying
            && !player.isPassenger() && player.isInWater()
            && GranulesEnchantments.level(player.getItemBySlot(EquipmentSlot.FEET), GranulesEnchantments.NECROSOLES) > 0;
    }
}
