package com.protoano.granules.tea;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public final class TeaShrubBlock extends SweetBerryBushBlock {
    public static final MapCodec<SweetBerryBushBlock> CODEC = simpleCodec(TeaShrubBlock::new);

    public TeaShrubBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SweetBerryBushBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(TeaContent.LEAVES);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) <= 1) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (level instanceof ServerLevel server) {
            dropFromBlockInteractLootTable(server, TeaContent.HARVEST, state, level.getBlockEntity(pos), null, player, (dropLevel, stack) -> {
                popResource(dropLevel, pos, stack);
            });
            server.playSound(null, pos, TeaContent.HARVEST_SOUND, SoundSource.BLOCKS, 1.0F, 0.8F + server.getRandom().nextFloat() * 0.4F);
            BlockState harvested = state.setValue(AGE, 1);
            server.setBlock(pos, harvested, 2);
            server.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, harvested));
        }
        return InteractionResult.SUCCESS;
    }
}
