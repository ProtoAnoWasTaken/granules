package com.protoano.granules.item;

import com.protoano.granules.sound.GranulesSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class EnchantedTomeItem extends Item {
	private static final String KNOWLEDGE_UNLOCKED_KEY = "KnowledgeTomeUnlocked";
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> KNOWLEDGE =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(
			net.minecraft.resources.Identifier.fromNamespaceAndPath("granules", "recipe_browser_unlocked"),
			builder -> builder.initializer(() -> false).persistent(com.mojang.serialization.Codec.BOOL)
				.copyOnDeath().syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL,
					net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.targetOnly()));

	public EnchantedTomeItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 60;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			revealRecipes(player);
			if (hasUnlockedKnowledge(player)) {
				return stack;
			}
			player.setAttached(KNOWLEDGE, true);
			CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(KNOWLEDGE_UNLOCKED_KEY, true));
			player.playSound(GranulesSounds.TOME_UNLOCK, 0.7F, 1.25F);
			((net.minecraft.server.level.ServerLevel) level).sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D, player.getZ(), 24, 0.35D, 0.6D, 0.35D, 0.08D);
			player.sendSystemMessage(Component.translatable("item.granules.enchanted_tome.recipe_book_unlocked"), true);
		}
		return stack;
	}

	public static boolean hasUnlockedKnowledge(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(KNOWLEDGE_UNLOCKED_KEY, false);
	}

	public static boolean hasUnlockedKnowledge(net.minecraft.world.entity.player.Player player) {
		return player.getAttachedOrElse(KNOWLEDGE, false);
	}

	public static void revealRecipes(ServerPlayer player) {
		player.awardRecipes(player.level().getServer().getRecipeManager().getRecipes());
	}

	public static void initialize() {
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			refreshRecipes(handler.player);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
			if (success) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					refreshRecipes(player);
				}
			}
		});
	}

	public static void refreshRecipes(ServerPlayer player) {
		if (hasUnlockedKnowledge(player)) {
			revealRecipes(player);
			return;
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty()) {
				net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), stack);
			}
		}
	}
}
