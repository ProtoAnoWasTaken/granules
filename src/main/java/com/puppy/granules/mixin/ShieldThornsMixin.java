package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class ShieldThornsMixin {
    @Redirect(
        method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F"
        )
    )
    private float damageBlockedAttacker(LivingEntity defender, ServerLevel level, DamageSource source, float amount) {
        float blocked = defender.applyItemBlocking(level, source, amount);
        if (blocked <= 0.0F) {
            return blocked;
        }
        ItemStack shield = defender.getUseItem();
        int thorns = GranulesEnchantments.level(level, shield, Enchantments.THORNS);
        Entity attacker = source.getEntity();
        if (thorns > 0 && attacker instanceof LivingEntity livingAttacker && level.getRandom().nextFloat() < 0.15F * thorns) {
            float damage = Math.min(4.0F, 1.0F + level.getRandom().nextInt(4));
            livingAttacker.hurtServer(level, defender.damageSources().thorns(defender), damage);
            EquipmentSlot slot = defender.getMainHandItem() == shield ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            shield.hurtAndBreak(2, defender, slot);
        }
        return blocked;
    }
}
