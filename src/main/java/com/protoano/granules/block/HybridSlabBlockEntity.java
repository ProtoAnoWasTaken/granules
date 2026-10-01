package com.protoano.granules.block;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class HybridSlabBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    private Halves halves = new Halves(normalize(Blocks.STONE_SLAB.defaultBlockState(), SlabType.BOTTOM),
        normalize(Blocks.OAK_SLAB.defaultBlockState(), SlabType.TOP));

    public HybridSlabBlockEntity(BlockPos position, BlockState state) {
        super(HybridSlabs.BLOCK_ENTITY, position, state);
    }

    public Halves halves() {
        return halves;
    }

    public void setHalves(BlockState bottom, BlockState top) {
        halves = new Halves(normalize(bottom, SlabType.BOTTOM), normalize(top, SlabType.TOP));
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public Object getRenderData() {
        return halves;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("bottom", BlockState.CODEC, halves.bottom());
        output.store("top", BlockState.CODEC, halves.top());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        halves = new Halves(normalize(input.read("bottom", BlockState.CODEC).orElse(Blocks.STONE_SLAB.defaultBlockState()), SlabType.BOTTOM),
            normalize(input.read("top", BlockState.CODEC).orElse(Blocks.OAK_SLAB.defaultBlockState()), SlabType.TOP));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    private static BlockState normalize(BlockState state, SlabType half) {
        if (!(state.getBlock() instanceof SlabBlock) || state.hasBlockEntity()) {
            state = Blocks.STONE_SLAB.defaultBlockState();
        }
        return state.setValue(SlabBlock.TYPE, half).setValue(SlabBlock.WATERLOGGED, false);
    }

    public record Halves(BlockState bottom, BlockState top) {
    }
}
