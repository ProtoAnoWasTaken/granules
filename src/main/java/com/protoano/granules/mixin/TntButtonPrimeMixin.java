package com.protoano.granules.mixin;

import com.protoano.granules.bomb.ExplosiveTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class TntButtonPrimeMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void granules$primeWithButton(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (!callback.getReturnValue().consumesAction() || context.getPlayer() == null || !context.getPlayer().isCrouching()
            || !(context.getPlayer().getOffhandItem().getItem() instanceof BlockItem button)
            || !(button.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock)
            || !(context.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos position = context.getClickedPos();
        BlockState state = level.getBlockState(position);
        if (!state.is(ExplosiveTags.BLOCKS) && !state.getBlock().asItem().builtInRegistryHolder().is(ExplosiveTags.ITEMS)) {
            return;
        }
        var ignition = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT_AND_STEEL);
        var hit = new net.minecraft.world.phys.BlockHitResult(
            net.minecraft.world.phys.Vec3.atCenterOf(position), context.getClickedFace(), position, false);
        InteractionResult result = state.useItemOn(ignition, level, context.getPlayer(), context.getHand(), hit);
        if (!result.consumesAction() && result != InteractionResult.FAIL) {
            ignition.useOn(new net.minecraft.world.item.context.UseOnContext(
                level, context.getPlayer(), context.getHand(), ignition, hit));
        }
    }
}
