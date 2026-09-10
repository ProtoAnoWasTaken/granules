package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EnchantedCombatMixin {
    @Unique
    private float granules$lastWeaponDamage;

    @Unique
    private ItemStack granules$lastWeapon = ItemStack.EMPTY;

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float applyArmorCurses(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        int curseLevel = 0;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            curseLevel = armorLevel(level, self, GranulesEnchantments.CURSE_OF_PIERCING);
            amount *= 1.0F + 0.08F * curseLevel;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            curseLevel = armorLevel(level, self, GranulesEnchantments.CURSE_OF_CORROSION);
            amount *= 1.0F + 0.15F * curseLevel;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            curseLevel = armorLevel(level, self, GranulesEnchantments.CURSE_OF_COMBUSTION);
            amount *= 1.0F + 0.08F * curseLevel;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof Player) {
            ItemStack weapon = weapon(source, attacker);
            if (GranulesEnchantments.level(level, weapon, GranulesEnchantments.VAMPIRISM) > 0) {
                granules$lastWeaponDamage = amount;
                granules$lastWeapon = weapon.copy();
            }
        }
        return amount;
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void applyKillEnchantments(DamageSource source, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }
        ItemStack weapon = weapon(source, attacker);
        int vampirism = GranulesEnchantments.level(level, weapon, GranulesEnchantments.VAMPIRISM);
        if (vampirism > 0 && ItemStack.isSameItemSameComponents(weapon, granules$lastWeapon)) {
            float fraction = vampirism == 1 ? 0.10F : vampirism == 2 ? 0.25F : 0.50F;
            player.heal(granules$lastWeaponDamage * fraction);
        }
        int beheading = GranulesEnchantments.level(level, weapon, GranulesEnchantments.BEHEADING);
        float chance = beheading == 1 ? 0.10F : beheading == 2 ? 0.25F : beheading == 3 ? 0.35F : 0.0F;
        Item head = headFor(self);
        if (head != null && level.getRandom().nextFloat() < chance) {
            ItemStack headStack = new ItemStack(head);
            if (self instanceof ServerPlayer slainPlayer) {
                headStack.set(DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(slainPlayer.getGameProfile()));
            }
            self.spawnAtLocation(level, headStack);
        }
    }

    @Unique
    private static int armorLevel(ServerLevel level, LivingEntity entity, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        int total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                total += GranulesEnchantments.level(level, entity.getItemBySlot(slot), enchantment);
            }
        }
        return total;
    }

    @Unique
    private static ItemStack weapon(DamageSource source, Entity attacker) {
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            return arrow.getWeaponItem();
        }
        return attacker.getWeaponItem();
    }

    @Unique
    private static Item headFor(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (entity instanceof Player) {
            return Items.PLAYER_HEAD;
        }
        if (type == EntityTypes.SKELETON) {
            return Items.SKELETON_SKULL;
        }
        if (type == EntityTypes.WITHER_SKELETON) {
            return Items.WITHER_SKELETON_SKULL;
        }
        if (type == EntityTypes.ZOMBIE) {
            return Items.ZOMBIE_HEAD;
        }
        if (type == EntityTypes.CREEPER) {
            return Items.CREEPER_HEAD;
        }
        if (type == EntityTypes.PIGLIN) {
            return Items.PIGLIN_HEAD;
        }
        if (type == EntityTypes.ENDER_DRAGON) {
            return Items.DRAGON_HEAD;
        }
        return null;
    }
}


