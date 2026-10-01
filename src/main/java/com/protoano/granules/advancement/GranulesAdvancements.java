package com.protoano.granules.advancement;

import com.protoano.granules.GranulesMod;
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
import com.protoano.granules.rabbit.KillerRabbitAccess;

import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import com.protoano.granules.fletching.ArrowParts;

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
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			for (String tag : oldPlayer.entityTags()) {
				if (tag.startsWith(JOIN_DAY_PREFIX) || tag.startsWith(ELDER_GUARDIAN_PREFIX)
					|| tag.startsWith(NAME_PREFIX) || tag.startsWith(ARROW_PREFIX)) {
					newPlayer.addTag(tag);
				}
			}
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				checkFirstNightDeath(player);
			}
			if (entity.getType() == EntityTypes.ELDER_GUARDIAN && source.getEntity() instanceof ServerPlayer player) {
				award(player, "the_deep_end");
				AdvancementHolder advancement = player.level().getServer().getAdvancements().get(
					Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "rock_bottom")
				);
				if (advancement != null) {
					var progress = player.getAdvancements().getOrStartProgress(advancement);
					for (int index = 1; index <= 3; index++) {
						var criterion = progress.getCriterion("guardian_" + index);
						if (criterion != null && !criterion.isDone()) {
							awardCriterion(player, "rock_bottom", "guardian_" + index);
							break;
						}
					}
				}
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

	public static void awardCriterion(ServerPlayer player, String path, String criterion) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(
			Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path)
		);
		if (advancement != null) {
			player.getAdvancements().award(advancement, criterion);
		}
	}

	public static void recordSomnosatchelLink(ServerPlayer player) {
		awardCriterion(player, "remote_access_bundle", "linked");
	}

    public static void recordLock(net.minecraft.world.entity.player.Player player, boolean locked) {
        if (player instanceof ServerPlayer serverPlayer) {
            awardCriterion(serverPlayer, "target_locked", locked ? "locked" : "unlocked");
        }
    }

	public static void recordSpecialName(ServerPlayer player, String name) {
		player.addTag(NAME_PREFIX + name);
		long count = player.entityTags().stream().filter(tag -> tag.startsWith(NAME_PREFIX)).count();
		if (count >= 3) {
			award(player, "meaningful_names");
		}
	}

	public static void recordArrowCombination(ServerPlayer player, String combination) {
		if (ArrowParts.fromIdentifierSuffix(combination).filter(parts -> !parts.isBasic()).isEmpty()) {
			return;
		}
		award(player, "you_have_my_bow");
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
			if (tag.startsWith(ARROW_PREFIX)
				&& player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "oh_fiddlesticks_what_now")) != null) {
				recordArrowCombination(player, tag.substring(ARROW_PREFIX.length()));
				player.removeTag(tag);
			}
		}
		long defeated = findNumber(player.entityTags(), ELDER_GUARDIAN_PREFIX);
		for (int index = 1; index <= Math.min(3L, defeated); index++) {
			awardCriterion(player, "rock_bottom", "guardian_" + index);
		}
		removeTags(player, ELDER_GUARDIAN_PREFIX);
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
