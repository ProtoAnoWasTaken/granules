package com.protoano.granules.grave;

import com.protoano.granules.rabbit.RabbitHoleContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class GraveLoot {
    public static final float CHEST_CHANCE = 0.05F;
    public static final float ZOMBIE_CHANCE = 0.01F;

    private GraveLoot() {
    }

    public static void chestLoot(Container container, ServerLevel level, long seed, long position) {
        chestLoot(container, level, seed, position, 0.0F);
    }

    public static void chestLoot(Container container, ServerLevel level, long seed, long position, float luck) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.GRAVE_SHOVEL)) {
            return;
        }
        if (!level.dimension().equals(RabbitHoleContent.BURROW) || !(container instanceof ChestBlockEntity)) {
            return;
        }
        RandomSource random = seed == 0 ? level.getRandom() : RandomSource.create(seed ^ position ^ 0x47524156454CL);
        if (random.nextFloat() >= com.protoano.granules.world.LuckAdjustedChance.adjusted(com.protoano.granules.config.BalanceConfig.Setting.GRAVE_CHEST_CHANCE.floatValue(), luck)) {
            return;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, new ItemStack(GraveContent.SHOVEL));
                container.setChanged();
                return;
            }
        }
    }

    public static boolean eligibleZombie(LivingEntity entity) {
        if (com.protoano.granules.config.ContentManifest.get().isBanned(com.protoano.granules.config.ContentManifest.Category.GRAVE_SHOVEL)) {
            return false;
        }
        if (entity.getType() != EntityTypes.ZOMBIE || !(entity.level() instanceof ServerLevel level)
            || !level.dimension().equals(RabbitHoleContent.BURROW)) {
            return false;
        }
        for (int y = 120; y >= 33; y--) {
            var pos = new net.minecraft.core.BlockPos(entity.getBlockX(), y, entity.getBlockZ());
            var state = level.getBlockState(pos);
            if ((state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.PODZOL)
                || state.is(net.minecraft.world.level.block.Blocks.MYCELIUM)) && level.getBlockState(pos.above()).isAir()) {
                return entity.getY() < y - 3;
            }
        }
        return entity.getY() < 32;
    }

    public static void equipZombie(net.minecraft.world.entity.monster.zombie.Zombie zombie, RandomSource random) {
        if (!eligibleZombie(zombie) || random.nextFloat() >= com.protoano.granules.config.BalanceConfig.Setting.GRAVE_ZOMBIE_CHANCE.floatValue()) {
            return;
        }
        ItemStack shovel = new ItemStack(GraveContent.SHOVEL);
        int minimumDamage = (int) Math.ceil(shovel.getMaxDamage() * 0.75);
        int maximumDamage = (int) Math.floor(shovel.getMaxDamage() * 0.95);
        shovel.setDamageValue(minimumDamage + random.nextInt(maximumDamage - minimumDamage + 1));
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, shovel);
    }
}
