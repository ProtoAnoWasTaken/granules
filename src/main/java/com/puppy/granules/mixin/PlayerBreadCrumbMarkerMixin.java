package com.puppy.granules.mixin;

import com.puppy.granules.world.BreadCrumbMarkerAccess;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerBreadCrumbMarkerMixin implements BreadCrumbMarkerAccess {
	@Unique
	private @Nullable GlobalPos granules$breadCrumbMarker;
	@Unique
	private @Nullable InteractionHand granules$breadCrumbEatingHand;
	@Unique
	private @Nullable ItemStack granules$breadCrumbEatingStack;
	@Unique
	private int granules$breadCrumbEatingDelay;

	@Override
	public @Nullable GlobalPos granules$getBreadCrumbMarker() {
		return granules$breadCrumbMarker;
	}

	@Override
	public void granules$setBreadCrumbMarker(@Nullable GlobalPos marker) {
		granules$breadCrumbMarker = marker;
	}

	@Override
	public void granules$beginBreadCrumbEating(InteractionHand hand, ItemStack stack) {
		granules$breadCrumbEatingHand = hand;
		granules$breadCrumbEatingStack = stack;
		granules$breadCrumbEatingDelay = 1;
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void granules$beginBreadCrumbEatUse(CallbackInfo callbackInfo) {
		if (granules$breadCrumbEatingHand == null || granules$breadCrumbEatingStack == null) {
			return;
		}
		if (granules$breadCrumbEatingDelay > 0) {
			granules$breadCrumbEatingDelay--;
			return;
		}
		Player player = (Player) (Object) this;
		InteractionHand hand = granules$breadCrumbEatingHand;
		ItemStack pendingStack = granules$breadCrumbEatingStack;
		granules$breadCrumbEatingHand = null;
		granules$breadCrumbEatingStack = null;
		if (player.getItemInHand(hand) == pendingStack && !player.isUsingItem()) {
			player.startUsingItem(hand);
			return;
		}
		pendingStack.remove(DataComponents.CONSUMABLE);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void granules$saveBreadCrumbMarker(ValueOutput output, CallbackInfo callbackInfo) {
		if (granules$breadCrumbMarker != null) {
			output.store("GranulesBreadCrumbMarker", GlobalPos.CODEC, granules$breadCrumbMarker);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void granules$loadBreadCrumbMarker(ValueInput input, CallbackInfo callbackInfo) {
		granules$breadCrumbMarker = input.read("GranulesBreadCrumbMarker", GlobalPos.CODEC).orElse(null);
	}
}
