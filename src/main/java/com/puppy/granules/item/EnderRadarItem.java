package com.puppy.granules.item;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.StrongholdRingLocator;
import com.puppy.granules.advancement.GranulesAdvancements;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ChunkPos;

public class EnderRadarItem extends Item {
	private static final String ON_MODEL = "on";
	private static final String PULSE_MODEL = "on_pulse";
	private static final String RING_KEY = "StrongholdRing";
	private static final String STRONGHOLD_KEY = "ExactStronghold";

	public EnderRadarItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		boolean accessible = isAccessible(player, stack, slot);
		StrongholdRingLocator.Detection detection = accessible
			? StrongholdRingLocator.detect(level, new ChunkPos(player.getBlockX() >> 4, player.getBlockZ() >> 4))
			: StrongholdRingLocator.Detection.OUTSIDE;
		boolean pulse = level.getGameTime() % 20L >= 10L;
		boolean inventoryChanged = setModel(stack, detection.isInRing());
		if (!isPrimaryRadar(player, stack)) {
			if (inventoryChanged) {
				syncInventory(player);
			}
			return;
		}
		TrackingState previousState = getTrackingState(stack);
		TrackingState currentState = new TrackingState(detection.ringIndex(), detection.exactStrongholdChunk());
		if (!previousState.equals(currentState)) {
			setTrackingState(stack, currentState);
			inventoryChanged = true;
		}
		if (inventoryChanged) {
			syncInventory(player);
		}
		if (detection.isInRing() && previousState.ringIndex() != detection.ringIndex()) {
			playEntranceSound(level, player);
		}
		if (detection.exactStrongholdChunk() && !previousState.exactStrongholdChunk()) {
			playEntranceSound(level, player);
			GranulesAdvancements.award(player, "somewhere_in_the_field");
		}
		if (detection.isInRing() && isDirectlyHeld(player, stack) && pulse && level.getGameTime() % 20L == 10L) {
			level.playSound(null, player.blockPosition(), GranulesMod.ENDER_RADAR_ACTIVE, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	private boolean isAccessible(ServerPlayer player, ItemStack stack, EquipmentSlot slot) {
		if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
			return true;
		}
		for (int inventorySlot = 0; inventorySlot < 9; inventorySlot++) {
			if (player.getInventory().getItem(inventorySlot) == stack) {
				return true;
			}
		}
		return false;
	}

	private boolean isPrimaryRadar(ServerPlayer player, ItemStack stack) {
		if (player.getMainHandItem().is(this)) {
			return player.getMainHandItem() == stack;
		}
		if (player.getOffhandItem().is(this)) {
			return player.getOffhandItem() == stack;
		}
		for (int inventorySlot = 0; inventorySlot < 9; inventorySlot++) {
			ItemStack hotbarStack = player.getInventory().getItem(inventorySlot);
			if (hotbarStack.is(this)) {
				return hotbarStack == stack;
			}
		}
		return false;
	}

	private boolean isDirectlyHeld(ServerPlayer player, ItemStack stack) {
		return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
	}

	private boolean setModel(ItemStack stack, boolean active) {
		if (!active) {
			if (!stack.has(DataComponents.CUSTOM_MODEL_DATA)) {
				return false;
			}
			stack.remove(DataComponents.CUSTOM_MODEL_DATA);
			return true;
		}
		CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		if (modelData != null && ON_MODEL.equals(modelData.getString(0))) {
			return false;
		}
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(ON_MODEL), List.of()));
		return true;
	}

	private TrackingState getTrackingState(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return TrackingState.OUTSIDE;
		}
		String ringValue = customData.copyTag().getStringOr(RING_KEY, "");
		int ringIndex = -1;
		if (!ringValue.isEmpty()) {
			try {
				ringIndex = Integer.parseInt(ringValue);
			} catch (NumberFormatException ignored) {
				ringIndex = -1;
			}
		}
		boolean exactStrongholdChunk = customData.copyTag().getBooleanOr(STRONGHOLD_KEY, false);
		return new TrackingState(ringIndex, exactStrongholdChunk);
	}

	private void setTrackingState(ItemStack stack, TrackingState state) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			if (state.ringIndex() < 0) {
				tag.remove(RING_KEY);
			} else {
				tag.putString(RING_KEY, Integer.toString(state.ringIndex()));
			}
			tag.putBoolean(STRONGHOLD_KEY, state.exactStrongholdChunk());
		});
	}

	private void playEntranceSound(ServerLevel level, ServerPlayer player) {
		level.playSound(null, player.blockPosition(), GranulesMod.ENDER_RADAR_ENTERED, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private void syncInventory(ServerPlayer player) {
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		if (player.containerMenu != player.inventoryMenu) {
			player.containerMenu.broadcastChanges();
		}
	}

	private record TrackingState(int ringIndex, boolean exactStrongholdChunk) {
		private static final TrackingState OUTSIDE = new TrackingState(-1, false);
	}
}
