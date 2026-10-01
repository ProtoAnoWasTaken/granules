package com.protoano.granules.mixin;

import com.protoano.granules.enchantment.GranulesEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
    private float trackWeaponDamage(float amount, ServerLevel level, DamageSource source) {
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
            float fraction = vampirism == 1 ? com.protoano.granules.config.BalanceConfig.Setting.VAMPIRISM_I_FRACTION.floatValue()
                : vampirism == 2 ? com.protoano.granules.config.BalanceConfig.Setting.VAMPIRISM_II_FRACTION.floatValue()
                : com.protoano.granules.config.BalanceConfig.Setting.VAMPIRISM_III_FRACTION.floatValue();
            player.heal(granules$lastWeaponDamage * fraction);
        }
        int beheading = GranulesEnchantments.level(level, weapon, GranulesEnchantments.BEHEADING);
        float chance = beheading == 1 ? com.protoano.granules.config.BalanceConfig.Setting.BEHEADING_I_CHANCE.floatValue()
            : beheading == 2 ? com.protoano.granules.config.BalanceConfig.Setting.BEHEADING_II_CHANCE.floatValue()
            : beheading == 3 ? com.protoano.granules.config.BalanceConfig.Setting.BEHEADING_III_CHANCE.floatValue() : 0.0F;
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


