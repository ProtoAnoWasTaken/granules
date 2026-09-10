package com.puppy.granules.world;

import com.puppy.granules.access.ServerPlayerGameModeAccess;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class CryingObsidianDrain {
	private static final int DRAIN_TIME = 600;
	private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

	private CryingObsidianDrain() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick(server.getPlayerList().getPlayers()));
	}

	private static void tick(Iterable<ServerPlayer> players) {
		Map<UUID, Boolean> activePlayers = new HashMap<>();
		for (ServerPlayer player : players) {
			UUID id = player.getUUID();
			activePlayers.put(id, true);
			ServerPlayerGameModeAccess gameMode = (ServerPlayerGameModeAccess) player.gameMode;
			BlockPos pos = gameMode.granules$getDestroyPos();
			if (!gameMode.granules$isDestroyingBlock() || pos == null || !player.level().getBlockState(pos).is(Blocks.CRYING_OBSIDIAN) || !hasAllowedPickaxe(player.getMainHandItem())) {
				PROGRESS.remove(id);
				continue;
			}
			Progress progress = PROGRESS.get(id);
			if (progress == null || !progress.pos().equals(pos) || progress.level() != player.level()) {
				PROGRESS.put(id, new Progress(player.level(), pos.immutable(), 1));
				continue;
			}
			int ticks = progress.ticks() + 1;
			if (ticks < DRAIN_TIME) {
				PROGRESS.put(id, new Progress(player.level(), pos.immutable(), ticks));
				continue;
			}
			player.level().levelEvent(2001, pos, Block.getId(Blocks.CRYING_OBSIDIAN.defaultBlockState()));
			player.level().setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
			player.level().playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
			PROGRESS.remove(id);
		}
		Iterator<UUID> iterator = PROGRESS.keySet().iterator();
		while (iterator.hasNext()) {
			if (!activePlayers.containsKey(iterator.next())) {
				iterator.remove();
			}
		}
	}

	private static boolean hasAllowedPickaxe(ItemStack stack) {
		return stack.is(Items.STONE_PICKAXE) || stack.is(Items.COPPER_PICKAXE) || stack.is(Items.IRON_PICKAXE);
	}

	private record Progress(Object level, BlockPos pos, int ticks) {
	}
}
