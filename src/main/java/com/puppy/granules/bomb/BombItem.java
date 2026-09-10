package com.puppy.granules.bomb;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

public class BombItem extends Item implements ProjectileItem {
    public static final float HALF_CHARGE_POWER = 1.5F + BowItem.getPowerForTime(10) * 1.5F;

    public BombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int heldTicks = this.getUseDuration(stack, entity) - remainingTime;
        float charge = BowItem.getPowerForTime(heldTicks);
        float power = 1.5F + charge * 1.5F;
        level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.SNOWBALL_THROW,
            SoundSource.NEUTRAL,
            0.5F,
            0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        if (level instanceof ServerLevel serverLevel) {
            ItemStack thrown = stack.consumeAndReturn(1, player);
            BombProjectile projectile = Projectile.spawnProjectileFromRotation(
                BombProjectile::new,
                serverLevel,
                thrown,
                player,
                0.0F,
                power,
                1.0F
            );
            projectile.setFullyCharged(charge >= 1.0F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        return new BombProjectile(level, position.x(), position.y(), position.z(), stack);
    }

    @Override
    public DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
            .power(HALF_CHARGE_POWER)
            .uncertainty(1.0F)
            .build();
    }
}
