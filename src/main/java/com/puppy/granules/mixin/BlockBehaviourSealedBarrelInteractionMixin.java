package com.puppy.granules.mixin;

import com.puppy.granules.world.SealedBarrelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourSealedBarrelInteractionMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void granules$sealOrUnsealBarrel(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (!(state.getBlock() instanceof BarrelBlock)) {
			return;
		}
		boolean sealed = state.getValue(SealedBarrelProperties.SEALED);
		if (!sealed && stack.is(Items.SLIME_BALL) && hitResult.getDirection() == state.getValue(BarrelBlock.FACING)) {
			if (!level.isClientSide()) {
				stack.consume(1, player);
				setSealed(level, pos, state, true);
				level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
			}
			callbackInfo.setReturnValue(InteractionResult.SUCCESS);
			return;
		}
		if (sealed && stack.getItem() instanceof AxeItem) {
			if (!level.isClientSide()) {
				setSealed(level, pos, state, false);
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				if (level.getRandom().nextBoolean()) {
					Block.popResource(level, pos.relative(hitResult.getDirection()), new ItemStack(Items.SLIME_BALL));
				}
				level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 0.8F, 1.0F);
			}
			callbackInfo.setReturnValue(InteractionResult.SUCCESS);
		}
	}

	private static void setSealed(Level level, BlockPos pos, BlockState state, boolean sealed) {
		level.setBlockAndUpdate(pos, state.setValue(SealedBarrelProperties.SEALED, sealed));
	}
}
