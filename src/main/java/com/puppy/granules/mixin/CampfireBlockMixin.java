package com.puppy.granules.mixin;

import com.puppy.granules.world.CampfireTanning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void placeRottenFleshForTanning(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (state.getValue(CampfireBlock.LIT) || !stack.is(Items.ROTTEN_FLESH)) {
			return;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof CampfireBlockEntity campfire)) {
			return;
		}
		if (level instanceof ServerLevel serverLevel && CampfireTanning.place(serverLevel, pos, state, campfire, player, stack)) {
			callbackInfo.setReturnValue(InteractionResult.SUCCESS_SERVER);
			return;
		}
		callbackInfo.setReturnValue(InteractionResult.CONSUME);
	}
}
