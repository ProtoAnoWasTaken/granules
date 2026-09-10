package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class EnchantedFishingHookMixin {
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void applyRodCombatEnchantments(EntityHitResult result, CallbackInfo callback) {
        FishingHook self = (FishingHook) (Object) this;
        Player owner = self.getPlayerOwner();
        if (owner == null || !(owner.level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        ItemStack rod = enchantedRod(owner);
        int punch = GranulesEnchantments.level(level, rod, Enchantments.PUNCH);
        if (punch > 0) {
            double x = owner.getX() - target.getX();
            double z = owner.getZ() - target.getZ();
            target.knockback(punch * 0.5D, x, z, owner.damageSources().playerAttack(owner), 1.0F, false);
        }
        if (GranulesEnchantments.level(level, rod, Enchantments.FLAME) > 0) {
            target.igniteForSeconds(5.0F);
        }
        int power = GranulesEnchantments.level(level, rod, Enchantments.POWER);
        if (power > 0) {
            target.hurtServer(level, owner.damageSources().thrown(self, owner), power);
        }
    }

    @ModifyConstant(method = "shouldStopFishing", constant = @Constant(doubleValue = 1024.0D))
    private double allowInfiniteLineDistance(double maximumDistanceSquared, Player player) {
        ItemStack rod = enchantedRod(player);
        if (player.level() instanceof ServerLevel level && GranulesEnchantments.level(level, rod, Enchantments.INFINITY) > 0) {
            return Double.MAX_VALUE;
        }
        return maximumDistanceSquared;
    }

    private static ItemStack enchantedRod(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(Items.FISHING_ROD)) {
            return mainHand;
        }
        return player.getOffhandItem();
    }
}

