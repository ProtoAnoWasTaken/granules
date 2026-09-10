package com.puppy.granules.rabbit;

import com.mojang.serialization.MapCodec;
import com.puppy.granules.config.ContentManifest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class RabbitHoleBlock extends BaseEntityBlock {
    public static final BooleanProperty PRIMED = BooleanProperty.create("primed");
    public static final MapCodec<RabbitHoleBlock> CODEC = simpleCodec(RabbitHoleBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public RabbitHoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PRIMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRIMED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RabbitHoleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, RabbitHoleContent.BLOCK_ENTITY, RabbitHoleBlockEntity::tick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.PODZOL) || below.is(Blocks.MYCELIUM);
    }

    @Override
    protected VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess tickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (player.getMainHandItem().is(ItemTags.SHOVELS)) {
            return 1.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.getMainHandItem().is(ItemTags.SHOVELS)) {
            level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
            return InteractionResult.PASS;
        }
        if (stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide()) {
                ItemStack rabbitHole = new ItemStack(RabbitHoleContent.ITEM);
                if (!player.addItem(rabbitHole)) {
                    Block.popResource(level, pos, rabbitHole);
                }
                level.removeBlock(pos, false);
                level.playSound(null, pos, RabbitHoleContent.SCOOP_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        boolean needsRing = !level.dimension().equals(RabbitHoleContent.BURROW);
        if (!stack.is(Items.GOLDEN_CARROT)
            || state.getValue(PRIMED)
            || needsRing && !hasMushroomRing(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RabbitHoleBlockEntity hole) {
            stack.consume(1, player);
            hole.prime((ServerLevel) level);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (ContentManifest.get().isBanned(ContentManifest.Category.THE_BURROW)) {
            return InteractionResult.PASS;
        }
        if (!state.getValue(PRIMED) || !(player instanceof ServerPlayer serverPlayer)) {
            return state.getValue(PRIMED) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof RabbitHoleBlockEntity hole) {
            hole.teleport(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean hasMushroomRing(LevelReader level, BlockPos pos) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2 || Math.abs(x) == 2 && Math.abs(z) == 2) {
                    continue;
                }
                BlockState state = level.getBlockState(pos.offset(x, 0, z));
                if (!state.is(Blocks.RED_MUSHROOM) && !state.is(Blocks.BROWN_MUSHROOM)) {
                    return false;
                }
            }
        }
        return true;
    }
}
