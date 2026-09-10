package com.puppy.granules.mixin;

import com.puppy.granules.world.SealedBarrelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BarrelBlock.class)
public abstract class BarrelBlockMixin {
	@Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
	private void granules$addSealedState(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo callbackInfo) {
		builder.add(SealedBarrelProperties.SEALED);
	}

	@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
	private void granules$placeSealedBarrel(BlockPlaceContext context, CallbackInfoReturnable<BlockState> callbackInfo) {
		BlockState state = callbackInfo.getReturnValue();
		CustomData data = context.getItemInHand().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		if (state != null && data.copyTag().getBooleanOr(SealedBarrelProperties.DATA_KEY, false)) {
			callbackInfo.setReturnValue(state.setValue(SealedBarrelProperties.SEALED, true));
		}
	}

	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void granules$denyOpening(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (state.getValue(SealedBarrelProperties.SEALED)) {
			callbackInfo.setReturnValue(InteractionResult.CONSUME);
		}
	}

}
