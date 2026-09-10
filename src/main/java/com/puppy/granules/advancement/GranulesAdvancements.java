package com.puppy.granules.advancement;

import com.puppy.granules.GranulesMod;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.clock.WorldClocks;
import com.puppy.granules.rabbit.KillerRabbitAccess;

import java.util.Set;

public final class GranulesAdvancements {
	private static final String JOIN_DAY_PREFIX = "granules_join_day_";
	private static final String ELDER_GUARDIAN_PREFIX = "granules_elder_guardian_";
	private static final String NAME_PREFIX = "granules_special_name_";
	private static final String ARROW_PREFIX = "granules_arrow_combination_";
	private static final String BURROW_ORIGIN_PREFIX = "granules_burrow_origin_";

	private GranulesAdvancements() {
	}

	public static void initialize() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			rememberJoinDay(handler.getPlayer());
			restoreTrackedProgress(handler.getPlayer());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				checkFirstNightDeath(player);
			}
			if (entity.getType() == EntityTypes.ELDER_GUARDIAN && source.getEntity() instanceof ServerPlayer player) {
				award(player, "the_deep_end");
				int defeated = increment(player, ELDER_GUARDIAN_PREFIX, 3);
				awardCriterion(player, "rock_bottom", "guardian_" + defeated);
			}
			if (entity instanceof KillerRabbitAccess rabbit
				&& rabbit.granules$dropsPalePelt()
				&& source.getEntity() instanceof ServerPlayer player) {
				award(player, "one_two_five");
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				var monument = player.level().structureManager().getStructureWithPieceAt(
					player.blockPosition(),
					holder -> holder.is(BuiltinStructures.OCEAN_MONUMENT)
				);
				if (monument.isValid()) {
					award(player, "twenty_three_leagues_under_the_sea");
				}
			}
		});
	}

	public static void award(ServerPlayer player, String path) {
		awardCriterion(player, path, "triggered");
	}

	private static void awardCriterion(ServerPlayer player, String path, String criterion) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(
			Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path)
		);
		if (advancement != null) {
			player.getAdvancements().award(advancement, criterion);
		}
	}

	public static void recordSpecialName(ServerPlayer player, String name) {
		player.addTag(NAME_PREFIX + name);
		long count = player.entityTags().stream().filter(tag -> tag.startsWith(NAME_PREFIX)).count();
		if (count >= 3) {
			award(player, "meaningful_names");
		}
	}

	public static void recordArrowCombination(ServerPlayer player, String combination, int total) {
		award(player, "you_have_my_bow");
		player.addTag(ARROW_PREFIX + combination);
		awardCriterion(player, "oh_fiddlesticks_what_now", combination);
	}

	public static void enterBurrow(ServerPlayer player, int overworldX, int overworldZ) {
		removeTags(player, BURROW_ORIGIN_PREFIX);
		player.addTag(BURROW_ORIGIN_PREFIX + overworldX + "_" + overworldZ);
		award(player, "call_me_paquerette");
	}

	public static void leaveBurrow(ServerPlayer player, int overworldX, int overworldZ) {
		for (String tag : Set.copyOf(player.entityTags())) {
			if (!tag.startsWith(BURROW_ORIGIN_PREFIX)) {
				continue;
			}
			String[] coordinates = tag.substring(BURROW_ORIGIN_PREFIX.length()).split("_");
			if (coordinates.length == 2) {
				try {
					double x = overworldX - Integer.parseInt(coordinates[0]);
					double z = overworldZ - Integer.parseInt(coordinates[1]);
					double distance = Math.sqrt(x * x + z * z);
					if (distance >= 500.0D) {
						award(player, "lessons_in_futility");
					}
					if (distance >= 3000.0D) {
						award(player, "big_walk");
					}
				} catch (NumberFormatException exception) {
					removeTags(player, BURROW_ORIGIN_PREFIX);
				}
			}
		}
		removeTags(player, BURROW_ORIGIN_PREFIX);
	}

	private static void restoreTrackedProgress(ServerPlayer player) {
		for (String tag : Set.copyOf(player.entityTags())) {
			if (tag.startsWith(ARROW_PREFIX)) {
				awardCriterion(player, "oh_fiddlesticks_what_now", tag.substring(ARROW_PREFIX.length()));
			}
		}
		long defeated = findNumber(player.entityTags(), ELDER_GUARDIAN_PREFIX);
		for (int index = 1; index <= Math.min(3L, defeated); index++) {
			awardCriterion(player, "rock_bottom", "guardian_" + index);
		}
	}

	private static void rememberJoinDay(ServerPlayer player) {
		if (findNumber(player.entityTags(), JOIN_DAY_PREFIX) < 0) {
			player.addTag(JOIN_DAY_PREFIX + overworldTime(player) / 24000L);
		}
	}

	private static void checkFirstNightDeath(ServerPlayer player) {
		long joinedDay = findNumber(player.entityTags(), JOIN_DAY_PREFIX);
		long dayTime = overworldTime(player);
		long currentDay = dayTime / 24000L;
		long timeOfDay = Math.floorMod(dayTime, 24000L);
		if (joinedDay == currentDay && timeOfDay >= 13000L && timeOfDay < 23000L) {
			award(player, "never_that_bad");
		}
	}

	private static int increment(ServerPlayer player, String prefix, int maximum) {
		long current = findNumber(player.entityTags(), prefix);
		if (current >= 0) {
			player.removeTag(prefix + current);
		}
		int next = (int) Math.min(maximum, Math.max(0L, current) + 1L);
		player.addTag(prefix + next);
		return next;
	}

	private static long findNumber(Set<String> tags, String prefix) {
		for (String tag : tags) {
			if (tag.startsWith(prefix)) {
				try {
					return Long.parseLong(tag.substring(prefix.length()));
				} catch (NumberFormatException exception) {
					return -1;
				}
			}
		}
		return -1;
	}

	private static void removeTags(ServerPlayer player, String prefix) {
		for (String tag : Set.copyOf(player.entityTags())) {
			if (tag.startsWith(prefix)) {
				player.removeTag(tag);
			}
		}
	}

	private static long overworldTime(ServerPlayer player) {
		var clock = player.level().registryAccess()
			.lookupOrThrow(Registries.WORLD_CLOCK)
			.getOrThrow(WorldClocks.OVERWORLD);
		return player.level().clockManager().getTotalTicks(clock);
	}
}
