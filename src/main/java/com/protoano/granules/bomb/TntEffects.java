package com.protoano.granules.bomb;

import com.protoano.granules.advancement.GranulesAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public final class TntEffects {
    public static final net.minecraft.world.level.ExplosionDamageCalculator DAMAGE_CALCULATOR = new net.minecraft.world.level.ExplosionDamageCalculator() {
        @Override
        public boolean shouldBlockExplode(net.minecraft.world.level.Explosion explosion, net.minecraft.world.level.BlockGetter level,
            BlockPos position, BlockState state, float power) {
            return state.getBlock() instanceof net.minecraft.world.level.block.TntBlock;
        }
    };

    private TntEffects() {
    }

    public static void apply(PrimedTnt tnt) {
        if (!(tnt.level() instanceof ServerLevel level)) {
            return;
        }
        if (tnt.getBlockState().is(BombContent.DIRT_TNT)) {
            primeNearbyTnt(level, tnt, com.protoano.granules.config.BalanceConfig.Setting.DIRT_TNT_RADIUS.intValue());
            replaceWithDirt(level, tnt.blockPosition(), com.protoano.granules.config.BalanceConfig.Setting.DIRT_TNT_RADIUS.intValue());
        }
        if (tnt.getBlockState().is(BombContent.DRY_TNT)) {
            primeNearbyTnt(level, tnt, 4);
            dryLiquids(level, tnt.blockPosition(), com.protoano.granules.config.BalanceConfig.Setting.DRY_TNT_RADIUS.intValue(), tnt);
        }
    }

    private static void primeNearbyTnt(ServerLevel level, PrimedTnt source, int radius) {
        BlockPos center = source.blockPosition();
        for (BlockPos position : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (center.distSqr(position) <= radius * radius) {
                GranulesTntBlock.primeFromBlast(level, position.immutable(), source.getOwner());
            }
        }
    }

    private static void replaceWithDirt(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos position : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (center.distSqr(position) <= radius * radius) {
                BlockState state = level.getBlockState(position);
                BlockEntity blockEntity = level.getBlockEntity(position);
                if (blockEntity == null && !PoiTypes.hasPoi(state)
                    && !(state.getBlock() instanceof net.minecraft.world.level.block.TntBlock)) {
                    level.setBlockAndUpdate(position.immutable(), Blocks.DIRT.defaultBlockState());
                }
            }
        }
    }

    private static void dryLiquids(ServerLevel level, BlockPos center, int radius, PrimedTnt tnt) {
        boolean removedLiquid = false;
        for (BlockPos position : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (center.distSqr(position) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(position);
            if (state.getFluidState().isEmpty()) {
                continue;
            }
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                level.setBlockAndUpdate(position.immutable(), state.setValue(BlockStateProperties.WATERLOGGED, false));
            } else {
                level.setBlockAndUpdate(position.immutable(), Blocks.AIR.defaultBlockState());
            }
            removedLiquid = true;
        }
        if (removedLiquid && tnt.getOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
            GranulesAdvancements.award(player, "sponge_bomb_square_pit");
        }
    }
}
