package com.protoano.granules.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class HybridSlabBlock extends BaseEntityBlock {
    private static final MapCodec<HybridSlabBlock> CODEC = simpleCodec(HybridSlabBlock::new);

    public HybridSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new HybridSlabBlockEntity(position, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos position) {
        if (level.getBlockEntity(position) instanceof HybridSlabBlockEntity hybrid) {
            return Math.min(hybrid.halves().bottom().getDestroyProgress(player, level, position),
                hybrid.halves().top().getDestroyProgress(player, level, position));
        }
        return super.getDestroyProgress(state, player, level, position);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HybridSlabBlockEntity hybrid) {
            ItemInstance tool = params.getOptionalParameter(LootContextParams.TOOL);
            var mining = tool == null ? null : tool.get(DataComponents.TOOL);
            boolean minedByPlayer = params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player;
            for (BlockState half : List.of(hybrid.halves().bottom(), hybrid.halves().top())) {
                if (!minedByPlayer || !half.requiresCorrectToolForDrops() || mining != null && mining.isCorrectForDrops(half)) {
                    drops.addAll(half.getDrops(params.withParameter(LootContextParams.BLOCK_STATE, half)));
                }
            }
        }
        return drops;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos position, BlockState state, boolean includeData) {
        if (level.getBlockEntity(position) instanceof HybridSlabBlockEntity hybrid) {
            ItemStack stack = new ItemStack(HybridSlabs.ITEM);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.TypedEntityData.of(
                HybridSlabs.BLOCK_ENTITY, hybrid.saveCustomOnly(level.registryAccess())));
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
