package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class FireAspectBlockDropsMixin {
    @Inject(
        method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void smeltBlockDrops(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        BlockEntity blockEntity,
        Entity entity,
        net.minecraft.world.item.ItemInstance tool,
        CallbackInfoReturnable<List<ItemStack>> callback
    ) {
        if (tool instanceof ItemStack stack) {
            callback.setReturnValue(GranulesEnchantments.smeltDrops(level, stack, callback.getReturnValue()));
        }
    }
}

