package com.puppy.granules.disc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Rarity;
import org.jspecify.annotations.Nullable;

public class UnmarkedDiscItem extends Item {
    public UnmarkedDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE) && stack.get(DataComponents.RARITY) != Rarity.EPIC) {
            stack.set(DataComponents.RARITY, Rarity.EPIC);
        }
        DiscContent.colorBlank(stack, level);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            DiscContent.colorBlank(stack, serverLevel);
        }
    }
}
