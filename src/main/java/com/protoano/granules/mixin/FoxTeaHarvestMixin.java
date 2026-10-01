package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.protoano.granules.tea.TeaContent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$FoxEatBerriesGoal")
public abstract class FoxTeaHarvestMixin {
    @WrapOperation(method = {"isValidTarget", "onReachedTarget"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
    private boolean granules$recognizeTea(BlockState state, Object block, Operation<Boolean> original) {
        return original.call(state, block) || block == Blocks.SWEET_BERRY_BUSH && state.is(TeaContent.SHRUB);
    }

    @ModifyExpressionValue(method = "pickSweetBerries", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/Items;SWEET_BERRIES:Lnet/minecraft/world/item/Item;"))
    private Item granules$harvestTea(Item original, BlockState state) {
        return state.is(TeaContent.SHRUB) ? TeaContent.LEAVES : original;
    }

    @ModifyExpressionValue(method = "pickSweetBerries", at = @At(value = "FIELD", target = "Lnet/minecraft/sounds/SoundEvents;SWEET_BERRY_BUSH_PICK_BERRIES:Lnet/minecraft/sounds/SoundEvent;"))
    private net.minecraft.sounds.SoundEvent granules$teaHarvestSound(net.minecraft.sounds.SoundEvent original, BlockState state) {
        return state.is(TeaContent.SHRUB) ? TeaContent.HARVEST_SOUND : original;
    }
}
