package com.protoano.granules.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class GildedTongsItem extends Item {
    public static final TagKey<Block> HARVESTABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("granules", "tongs_harvestable"));

    public GildedTongsItem(Properties properties) {
        super(properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(HARVESTABLE) ? 5.0F : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(HARVESTABLE);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide() && state.is(HARVESTABLE)) {
            stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND);
        }
        return state.is(HARVESTABLE);
    }

    public static ItemStack harvestTool(ItemStack tool, BlockState state, Level level) {
        if (!(tool.getItem() instanceof GildedTongsItem) || !state.is(HARVESTABLE)) {
            return tool;
        }
        ItemStack copy = tool.copy();
        copy.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        return copy;
    }
}
