package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyCauldrons;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCauldronBlock.class)
public abstract class AbstractCauldronHoneyMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void granules$handleHoney(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (!state.is(Blocks.CAULDRON)) {
			return;
		}

		int honeyLevel = HoneyCauldrons.getHoneyLevel(state);
		if (itemStack.is(GranulesMod.HONEY_BUCKET)) {
			if (honeyLevel == 3) {
				callbackInfo.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
				return;
			}

			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.BUCKET)));
				player.awardStat(Stats.FILL_CAULDRON);
				player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
				BlockState newState = HoneyCauldrons.withHoneyLevel(state, 3);
				level.setBlockAndUpdate(pos, newState);
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
			}

			callbackInfo.setReturnValue(InteractionResult.SUCCESS);
			return;
		}

		if (honeyLevel == 0) {
			return;
		}

		if (itemStack.is(Items.BUCKET) && honeyLevel == 3) {
			if (!level.isClientSide()) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(GranulesMod.HONEY_BUCKET)));
				player.awardStat(Stats.USE_CAULDRON);
				player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
				BlockState newState = HoneyCauldrons.withHoneyLevel(state, 0);
				level.setBlockAndUpdate(pos, newState);
				level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
			}

			callbackInfo.setReturnValue(InteractionResult.SUCCESS);
			return;
		}

		if (itemStack.is(ItemTags.SHOVELS) && honeyLevel == 3) {
			if (!level.isClientSide()) {
				if (!player.getInventory().add(new ItemStack(Blocks.HONEY_BLOCK))) {
					player.drop(new ItemStack(Blocks.HONEY_BLOCK), false);
				}
				itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				BlockState newState = HoneyCauldrons.withHoneyLevel(state, 0);
				level.setBlockAndUpdate(pos, newState);
				level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
			}

			callbackInfo.setReturnValue(InteractionResult.SUCCESS);
			return;
		}

		if (!itemStack.is(Items.GLASS_BOTTLE)) {
			callbackInfo.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
			return;
		}

		if (!level.isClientSide()) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.HONEY_BOTTLE)));
			player.awardStat(Stats.USE_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
			BlockState newState = HoneyCauldrons.withHoneyLevel(state, honeyLevel - 1);
			level.setBlockAndUpdate(pos, newState);
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		}

		callbackInfo.setReturnValue(InteractionResult.SUCCESS);
	}
}
