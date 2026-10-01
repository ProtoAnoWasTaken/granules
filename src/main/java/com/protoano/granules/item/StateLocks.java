package com.protoano.granules.item;

import com.protoano.granules.sound.GranulesSounds;
import com.protoano.granules.block.PlanterBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class StateLocks {
    public static final BooleanProperty LOCKED = BooleanProperty.create("granules_locked");

    private StateLocks() {
    }

    public static boolean supports(Block block) {
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock
            || block instanceof SaplingBlock || block instanceof SweetBerryBushBlock || block instanceof CactusBlock
            || block instanceof SugarCaneBlock || block instanceof BambooStalkBlock || block instanceof BambooSaplingBlock
            || block instanceof GrowingPlantBlock || block instanceof NetherWartBlock || block instanceof CocoaBlock
            || block instanceof ChorusFlowerBlock || block instanceof MushroomBlock || block instanceof NetherFungusBlock || block instanceof AzaleaBlock
            || block instanceof EyeblossomBlock || block instanceof FlowerPotBlock || block instanceof CoralBlock
            || block instanceof BaseCoralPlantTypeBlock || block instanceof PlanterBlock || block instanceof SculkCatalystBlock
            || block instanceof HopperBlock || block instanceof RepeaterBlock || block instanceof TurtleEggBlock
            || block instanceof SnifferEggBlock || block instanceof FrogspawnBlock;
    }

    public static boolean locked(BlockState state) {
        return state.hasProperty(LOCKED) && state.getValue(LOCKED);
    }

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!player.getItemInHand(hand).is(UtilityItems.IRON_LOCK) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            if (!level.getBlockState(hit.getBlockPos()).hasProperty(LOCKED)) {
                return InteractionResult.PASS;
            }
            return UtilityItems.IRON_LOCK.useOn(new UseOnContext(player, hand, hit));
        });
    }

    public static void toggle(Level level, BlockPos pos, BlockState state) {
        boolean locked = !locked(state);
        BlockState updated = state.setValue(LOCKED, locked);
        if (state.getBlock() instanceof HopperBlock) {
            updated = updated.setValue(HopperBlock.ENABLED, !locked && !level.hasNeighborSignal(pos));
        }
        if (state.getBlock() instanceof RepeaterBlock repeater) {
            updated = updated.setValue(RepeaterBlock.LOCKED, locked || repeater.isLocked(level, pos, updated));
        }
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        if (state.getBlock() instanceof CactusBlock || state.getBlock() instanceof SugarCaneBlock
            || state.getBlock() instanceof BambooStalkBlock || state.getBlock() instanceof PitcherCropBlock) {
            for (var direction : new net.minecraft.core.Direction[] {net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN}) {
                BlockPos adjacent = pos.relative(direction);
                while (!level.isOutsideBuildHeight(adjacent)) {
                    BlockState member = level.getBlockState(adjacent);
                    if (!member.is(state.getBlock()) || !member.hasProperty(LOCKED)) {
                        break;
                    }
                    level.setBlock(adjacent, member.setValue(LOCKED, locked), Block.UPDATE_ALL);
                    adjacent = adjacent.relative(direction);
                }
            }
        }
        level.playSound(null, pos, locked ? GranulesSounds.LOCK : GranulesSounds.UNLOCK, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!locked) {
            if (state.getBlock() instanceof SnifferEggBlock || state.getBlock() instanceof FrogspawnBlock) {
                updated.onPlace(level, pos, Blocks.AIR.defaultBlockState(), false);
            } else if (state.getBlock() instanceof RepeaterBlock || state.getBlock() instanceof EyeblossomBlock || state.getBlock() instanceof SculkCatalystBlock
                || state.getBlock() instanceof CoralBlock || state.getBlock() instanceof BaseCoralPlantTypeBlock) {
                level.scheduleTick(pos, state.getBlock(), 2);
            }
        }
    }

    public static boolean treeLocked(Level level, BlockPos pos, BlockState state) {
        if (locked(state)) {
            return true;
        }
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                BlockPos corner = pos.offset(x, 0, z);
                BlockState first = level.getBlockState(corner);
                BlockState second = level.getBlockState(corner.east());
                BlockState third = level.getBlockState(corner.south());
                BlockState fourth = level.getBlockState(corner.east().south());
                if (first.is(state.getBlock()) && second.is(state.getBlock()) && third.is(state.getBlock()) && fourth.is(state.getBlock())
                    && (locked(first) || locked(second) || locked(third) || locked(fourth))) {
                    return true;
                }
            }
        }
        return false;
    }
}
