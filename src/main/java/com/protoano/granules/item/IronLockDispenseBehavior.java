package com.protoano.granules.item;

import com.protoano.granules.sound.GranulesSounds;
import com.protoano.granules.mixin.CopperGolemLockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public final class IronLockDispenseBehavior extends OptionalDispenseItemBehavior {
    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        setSuccess(toggleTarget(source.level(), pos));
        if (isSuccess()) {
            stack.hurtAndBreak(1, source.level(), null, item -> {
            });
        }
        return stack;
    }

    private static boolean toggleTarget(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.hasProperty(StateLocks.LOCKED)) {
            StateLocks.toggle(level, pos, state);
            return true;
        }
        if (state.hasProperty(IronLockItem.INVERTED)) {
            IronLockItem.update(level, pos, state, !state.getValue(IronLockItem.INVERTED), null);
            return true;
        }
        for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(pos), target -> target.isAlive() && !target.isSpectator())) {
            if (entity instanceof CopperGolem golem) {
                if (level.getBlockState(golem.blockPosition()).isAir()) {
                    ((CopperGolemLockAccessor) golem).granules$turnToStatue(level);
                    return true;
                }
                continue;
            }
            String tag;
            if (entity instanceof Painting) {
                tag = PaintingLocks.LOCKED;
            } else if (EntityLocks.supports(entity)) {
                tag = EntityLocks.LOCKED;
            } else {
                continue;
            }
            boolean locked = entity.entityTags().contains(tag);
            if (locked) {
                entity.removeTag(tag);
            } else if (!entity.addTag(tag)) {
                continue;
            }
            entity.playSound(locked ? GranulesSounds.UNLOCK : GranulesSounds.LOCK, 1.0F, 1.0F);
            return true;
        }
        return false;
    }
}
