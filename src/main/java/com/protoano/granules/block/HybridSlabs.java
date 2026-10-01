package com.protoano.granules.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class HybridSlabs {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("granules", "hybrid_slab");
    public static final HybridSlabBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, ID,
        new HybridSlabBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ID))
            .strength(3.0F, 6.0F).sound(SoundType.STONE).pushReaction(PushReaction.BLOCK)));
    public static final BlockEntityType<HybridSlabBlockEntity> BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
        ID, FabricBlockEntityTypeBuilder.create(HybridSlabBlockEntity::new, BLOCK).build());

    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ID,
        new BlockItem(BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix()));
    private HybridSlabs() {
    }

    public static void initialize() {
    }

    public static InteractionResult tryCombine(BlockItem item, UseOnContext context) {
        if (!(item.getBlock() instanceof SlabBlock incoming)) {
            return InteractionResult.PASS;
        }
        var level = context.getLevel();
        BlockPos position = context.getClickedPos();
        BlockState existing = level.getBlockState(position);
        if (isSingleSlab(existing)) {
            SlabType occupied = existing.getValue(SlabBlock.TYPE);
            Direction face = context.getClickedFace();
            double height = context.getClickLocation().y - position.getY();
            boolean emptyHalf = occupied == SlabType.BOTTOM
                ? face == Direction.UP || face.getAxis().isHorizontal() && height > 0.5D
                : face == Direction.DOWN || face.getAxis().isHorizontal() && height < 0.5D;
            if (!emptyHalf) {
                return InteractionResult.PASS;
            }
        } else {
            BlockPlaceContext placement = new BlockPlaceContext(context);
            position = placement.getClickedPos();
            existing = level.getBlockState(position);
            BlockState proposed = incoming.getStateForPlacement(placement);
            if (!isSingleSlab(existing) || proposed == null || proposed.getValue(SlabBlock.TYPE) == existing.getValue(SlabBlock.TYPE)) {
                return InteractionResult.PASS;
            }
        }
        if (existing.is(incoming) || incoming.defaultBlockState().hasBlockEntity()) {
            return InteractionResult.PASS;
        }
        var player = context.getPlayer();
        if (player == null || context.getItemInHand().isEmpty() || !level.mayInteract(player, position)
            || !player.mayUseItemAt(position, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        BlockState combined = BLOCK.defaultBlockState();
        if (!level.isUnobstructed(combined, position, CollisionContext.of(player))) {
            return InteractionResult.FAIL;
        }
        boolean bottomOccupied = existing.getValue(SlabBlock.TYPE) == SlabType.BOTTOM;
        if (!level.setBlock(position, combined, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(position) instanceof HybridSlabBlockEntity hybrid) {
            hybrid.setHalves(bottomOccupied ? existing : incoming.defaultBlockState(),
                bottomOccupied ? incoming.defaultBlockState() : existing);
        }
        SoundType sound = incoming.defaultBlockState().getSoundType();
        level.playSound(player, position, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
            sound.getPitch() * 0.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, position, GameEvent.Context.of(player, combined));
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    private static boolean isSingleSlab(BlockState state) {
        return state.getBlock() instanceof SlabBlock && !state.hasBlockEntity() && state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE;
    }
}
