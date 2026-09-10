package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.EnchantedCauldronProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractCauldronBlock.class)
public abstract class EnchantedCauldronMixin {
	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void granules$handleEnchantedCapacity(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (!state.is(GranulesMod.ENCHANTED_CAULDRON)) {
			return;
		}
		if (itemStack.is(Items.WATER_BUCKET)) {
			granules$fillFromBucket(itemStack, state, level, pos, player, hand, EnchantedCauldronProperties.Content.WATER, 3, 9, SoundEvents.BUCKET_EMPTY, callbackInfo);
			return;
		}
		if (itemStack.is(GranulesMod.HONEY_BUCKET)) {
			granules$fillFromBucket(itemStack, state, level, pos, player, hand, EnchantedCauldronProperties.Content.HONEY, 3, 9, SoundEvents.BUCKET_EMPTY, callbackInfo);
			return;
		}
		if (itemStack.is(Items.LAVA_BUCKET)) {
			granules$fillFromBucket(itemStack, state, level, pos, player, hand, EnchantedCauldronProperties.Content.LAVA, 1, 3, SoundEvents.BUCKET_EMPTY_LAVA, callbackInfo);
			return;
		}
		if (itemStack.is(Items.POWDER_SNOW_BUCKET)) {
			granules$fillFromBucket(itemStack, state, level, pos, player, hand, EnchantedCauldronProperties.Content.POWDER_SNOW, 3, 9, SoundEvents.BUCKET_EMPTY_POWDER_SNOW, callbackInfo);
			return;
		}
		if (itemStack.is(Items.BUCKET)) {
			granules$fillBucket(itemStack, state, level, pos, player, hand, callbackInfo);
			return;
		}
		if (granules$isWaterPotion(itemStack)) {
			granules$addWaterPotion(itemStack, state, level, pos, player, hand, callbackInfo);
			return;
		}
		if (itemStack.is(Items.GLASS_BOTTLE)) {
			granules$fillBottle(itemStack, state, level, pos, player, hand, callbackInfo);
		}
	}

	private void granules$fillFromBucket(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		EnchantedCauldronProperties.Content content,
		int amount,
		int maximum,
		SoundEvent sound,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (!granules$canAdd(state, content, amount, maximum)) {
			callbackInfo.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
			return;
		}
		if (!level.isClientSide()) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.BUCKET)));
			player.awardStat(Stats.FILL_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
			level.setBlockAndUpdate(pos, granules$withContent(state, content, granules$getLevel(state) + amount));
			level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
		}
		callbackInfo.setReturnValue(InteractionResult.SUCCESS);
	}

	private void granules$fillBucket(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		EnchantedCauldronProperties.Content content = state.getValue(EnchantedCauldronProperties.CONTENT);
		int levelAmount = granules$getLevel(state);
		ItemStack filledBucket = switch (content) {
			case WATER -> levelAmount >= 3 ? new ItemStack(Items.WATER_BUCKET) : ItemStack.EMPTY;
			case HONEY -> levelAmount >= 3 ? new ItemStack(GranulesMod.HONEY_BUCKET) : ItemStack.EMPTY;
			case LAVA -> levelAmount >= 1 ? new ItemStack(Items.LAVA_BUCKET) : ItemStack.EMPTY;
			case POWDER_SNOW -> levelAmount >= 3 ? new ItemStack(Items.POWDER_SNOW_BUCKET) : ItemStack.EMPTY;
			case EMPTY -> ItemStack.EMPTY;
		};
		if (filledBucket.isEmpty()) {
			callbackInfo.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
			return;
		}
		int removed = content == EnchantedCauldronProperties.Content.LAVA ? 1 : 3;
		if (!level.isClientSide()) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, filledBucket));
			player.awardStat(Stats.USE_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
			level.setBlockAndUpdate(pos, granules$withContent(state, content, levelAmount - removed));
			level.playSound(null, pos, content == EnchantedCauldronProperties.Content.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		}
		callbackInfo.setReturnValue(InteractionResult.SUCCESS);
	}

	private void granules$addWaterPotion(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (!granules$canAdd(state, EnchantedCauldronProperties.Content.WATER, 1, 9)) {
			callbackInfo.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
			return;
		}
		if (!level.isClientSide()) {
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, new ItemStack(Items.GLASS_BOTTLE)));
			player.awardStat(Stats.USE_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
			level.setBlockAndUpdate(pos, granules$withContent(state, EnchantedCauldronProperties.Content.WATER, granules$getLevel(state) + 1));
			level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
		}
		callbackInfo.setReturnValue(InteractionResult.SUCCESS);
	}

	private void granules$fillBottle(
		ItemStack itemStack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		EnchantedCauldronProperties.Content content = state.getValue(EnchantedCauldronProperties.CONTENT);
		if ((content != EnchantedCauldronProperties.Content.WATER && content != EnchantedCauldronProperties.Content.HONEY) || granules$getLevel(state) == 0) {
			return;
		}
		if (!level.isClientSide()) {
			ItemStack filledBottle = content == EnchantedCauldronProperties.Content.WATER
				? PotionContents.createItemStack(Items.POTION, Potions.WATER)
				: new ItemStack(Items.HONEY_BOTTLE);
			player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, filledBottle));
			player.awardStat(Stats.USE_CAULDRON);
			player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
			level.setBlockAndUpdate(pos, granules$withContent(state, content, granules$getLevel(state) - 1));
			level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
		}
		callbackInfo.setReturnValue(InteractionResult.SUCCESS);
	}

	private boolean granules$isWaterPotion(ItemStack stack) {
		PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
		return stack.is(Items.POTION) && potionContents != null && potionContents.is(Potions.WATER);
	}

	private boolean granules$canAdd(BlockState state, EnchantedCauldronProperties.Content content, int amount, int maximum) {
		EnchantedCauldronProperties.Content currentContent = state.getValue(EnchantedCauldronProperties.CONTENT);
		return (currentContent == EnchantedCauldronProperties.Content.EMPTY || currentContent == content) && granules$getLevel(state) + amount <= maximum;
	}

	private int granules$getLevel(BlockState state) {
		return state.getValue(EnchantedCauldronProperties.LEVEL);
	}

	private BlockState granules$withContent(BlockState state, EnchantedCauldronProperties.Content content, int level) {
		EnchantedCauldronProperties.Content finalContent = level == 0 ? EnchantedCauldronProperties.Content.EMPTY : content;
		return state.setValue(EnchantedCauldronProperties.CONTENT, finalContent).setValue(EnchantedCauldronProperties.LEVEL, level);
	}
}
