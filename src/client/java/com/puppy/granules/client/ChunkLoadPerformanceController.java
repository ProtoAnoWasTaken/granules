package com.puppy.granules.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.PrioritizeChunkUpdates;

public final class ChunkLoadPerformanceController {
	private static final int WINDOW_TICKS = 20;
	private static final int CHUNK_BURST_THRESHOLD = 8;
	private static final int RECOVERY_TICKS = 80;

	private static int loadedChunksInWindow;
	private static int ticksInWindow;
	private static int quietTicks;
	private static boolean initialized;
	private static boolean enabled;
	private static boolean performanceModeActive;
	private static PrioritizeChunkUpdates previousChunkUpdatePriority;

	private ChunkLoadPerformanceController() {
	}

	public static void initialize(boolean initialEnabled) {
		if (initialized) {
			setEnabled(Minecraft.getInstance(), initialEnabled);
			return;
		}

		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> loadedChunksInWindow++);
		ClientTickEvents.END_CLIENT_TICK.register(ChunkLoadPerformanceController::tick);
		initialized = true;
		enabled = initialEnabled;
	}

	public static void setEnabled(Minecraft client, boolean enabled) {
		if (ChunkLoadPerformanceController.enabled == enabled) {
			return;
		}

		if (!enabled) {
			restoreSettings(client);
		}

		ChunkLoadPerformanceController.enabled = enabled;
		resetWindow();
	}

	private static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}

		if (client.level == null) {
			restoreSettings(client);
			resetWindow();
			return;
		}

		ticksInWindow++;
		if (ticksInWindow < WINDOW_TICKS) {
			return;
		}

		if (!performanceModeActive && loadedChunksInWindow >= CHUNK_BURST_THRESHOLD) {
			enablePerformanceMode(client);
		}

		if (performanceModeActive) {
			if (loadedChunksInWindow == 0) {
				quietTicks += WINDOW_TICKS;
			} else {
				quietTicks = 0;
			}

			if (quietTicks >= RECOVERY_TICKS) {
				restoreSettings(client);
			}
		}

		resetWindow();
	}

	private static void enablePerformanceMode(Minecraft client) {
		OptionInstance<PrioritizeChunkUpdates> chunkUpdatePriority = client.options.prioritizeChunkUpdates();
		previousChunkUpdatePriority = chunkUpdatePriority.get();
		chunkUpdatePriority.set(PrioritizeChunkUpdates.NEARBY);
		quietTicks = 0;
		performanceModeActive = true;
	}

	private static void restoreSettings(Minecraft client) {
		if (!performanceModeActive) {
			return;
		}

		OptionInstance<PrioritizeChunkUpdates> chunkUpdatePriority = client.options.prioritizeChunkUpdates();
		if (chunkUpdatePriority.get() == PrioritizeChunkUpdates.NEARBY) {
			chunkUpdatePriority.set(previousChunkUpdatePriority);
		}

		performanceModeActive = false;
		quietTicks = 0;
	}

	private static void resetWindow() {
		loadedChunksInWindow = 0;
		ticksInWindow = 0;
	}
}
