package com.puppy.granules.world;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CampfireTanning {
	private static final int TANNING_TIME = 2400;
	private static final Map<CampfireBlockEntity, int[]> PROGRESS = new WeakHashMap<>();

	private CampfireTanning() {
	}

	public static boolean place(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity campfire, Player player, ItemStack stack) {
		for (int slot = 0; slot < campfire.getItems().size(); slot++) {
			if (!campfire.getItems().get(slot).isEmpty()) {
				continue;
			}
			campfire.getItems().set(slot, stack.consumeAndReturn(1, player));
			progressFor(campfire)[slot] = 0;
			campfire.setChanged();
			level.sendBlockUpdated(pos, state, state, 3);
			return true;
		}
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
		if (!(level instanceof ServerLevel serverLevel) || state.getValue(CampfireBlock.LIT) || !level.isBrightOutside()) {
			return;
		}
		int[] progress = progressFor(campfire);
		for (int slot = 0; slot < campfire.getItems().size(); slot++) {
			ItemStack stack = campfire.getItems().get(slot);
			if (!stack.is(Items.ROTTEN_FLESH)) {
				progress[slot] = 0;
				continue;
			}
			progress[slot]++;
			if (progress[slot] < TANNING_TIME) {
				continue;
			}
			campfire.getItems().set(slot, ItemStack.EMPTY);
			progress[slot] = 0;
			Containers.dropItemStack(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, new ItemStack(Items.LEATHER));
			campfire.setChanged();
			serverLevel.sendBlockUpdated(pos, state, state, 3);
		}
	}

	private static int[] progressFor(CampfireBlockEntity campfire) {
		return PROGRESS.computeIfAbsent(campfire, ignored -> new int[4]);
	}
}
