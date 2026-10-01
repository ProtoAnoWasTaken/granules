package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.protoano.granules.item.PaintingLocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Painting.class)
public abstract class PaintingLockMixin {
    @WrapOperation(method = "dropItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/painting/Painting;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity granules$preservePainting(Painting painting, ServerLevel level, ItemLike item, Operation<ItemEntity> original) {
        if (painting.entityTags().contains(PaintingLocks.LOCKED)) {
            return painting.spawnAtLocation(level, PaintingLocks.fixedItem(painting));
        }
        return original.call(painting, level, item);
    }
}
