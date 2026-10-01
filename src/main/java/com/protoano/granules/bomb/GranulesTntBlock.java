package com.protoano.granules.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public final class GranulesTntBlock extends TntBlock {
    public GranulesTntBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos position, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(position) && prime(level, position, null)) {
            level.removeBlock(position, false);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos position, Block block, Orientation orientation, boolean movedByPiston) {
        if (level.hasNeighborSignal(position) && prime(level, position, null)) {
            level.removeBlock(position, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos position, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild && state.getValue(UNSTABLE)) {
            prime(level, position, null);
        }
        return super.playerWillDestroy(level, position, state.setValue(UNSTABLE, false), player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos position, Explosion explosion) {
        if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) {
            return;
        }
        PrimedTnt tnt = createPrimed(level, position, explosion.getIndirectSourceEntity());
        tnt.setBlockState(this.defaultBlockState());
        tnt.setFuse(PrimedTnt.getRandomShortFuse(tnt.getFuse(), level.getRandom()));
        level.addFreshEntity(tnt);
    }

    public static boolean prime(Level level, BlockPos position, LivingEntity source) {
        if (!(level instanceof ServerLevel serverLevel)
            || !serverLevel.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)
            || !(level.getBlockState(position).getBlock() instanceof GranulesTntBlock)) {
            return false;
        }
        PrimedTnt tnt = createPrimed(level, position, source);
        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(source, net.minecraft.world.level.gameevent.GameEvent.PRIME_FUSE, position);
        return true;
    }

    public static void primeFromBlast(ServerLevel level, BlockPos position, LivingEntity source) {
        if (!level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)
            || !(level.getBlockState(position).getBlock() instanceof TntBlock)) {
            return;
        }
        PrimedTnt tnt = createPrimed(level, position, source);
        tnt.setFuse(PrimedTnt.getRandomShortFuse(tnt.getFuse(), level.getRandom()));
        if (level.removeBlock(position, false)) {
            level.addFreshEntity(tnt);
        }
    }

    private static PrimedTnt createPrimed(Level level, BlockPos position, LivingEntity source) {
        PrimedTnt tnt = new PrimedTnt(level, position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, source);
        tnt.setBlockState(level.getBlockState(position));
        return tnt;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos position, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, position, player, hand, hit);
        }
        if (prime(level, position, player)) {
            level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
            Item item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (level instanceof ServerLevel serverLevel
            && !serverLevel.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLODES)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity owner = projectile.getOwner();
        LivingEntity source = owner instanceof LivingEntity livingEntity ? livingEntity : null;
        if (projectile.isOnFire() && projectile.mayInteract(serverLevel, hit.getBlockPos()) && prime(level, hit.getBlockPos(), source)) {
            level.removeBlock(hit.getBlockPos(), false);
        }
    }
}
