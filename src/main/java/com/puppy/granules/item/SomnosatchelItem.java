package com.puppy.granules.item;

import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class SomnosatchelItem extends Item {
	private static final String LINKED_KEY = "SomnosatchelLinked";

	public SomnosatchelItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.ENDER_CHEST)) {
			return super.useOn(context);
		}
		if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
			setLinked(context.getItemInHand());
			GranulesAdvancements.award(player, "remote_access_bundle");
			player.sendSystemMessage(Component.translatable("item.granules.somnosatchel.linked"), true);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.CONSUME;
		}
		if (!isLinked(stack)) {
			serverPlayer.sendSystemMessage(Component.translatable("item.granules.somnosatchel.unlinked"), true);
			return InteractionResult.CONSUME;
		}
		serverPlayer.openMenu(new SimpleMenuProvider(
			(containerId, inventory, openingPlayer) -> ChestMenu.threeRows(
				containerId,
				inventory,
				openingPlayer.getEnderChestInventory()
			),
			Component.translatable("container.enderchest")
		));
		serverPlayer.playSound(SoundEvents.ENDER_CHEST_OPEN, 0.5F, 1.0F);
		serverPlayer.awardStat(Stats.OPEN_ENDERCHEST);
		return InteractionResult.CONSUME;
	}

	public static boolean isLinked(ItemStack stack) {
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		return customData.copyTag().getBooleanOr(LINKED_KEY, false);
	}

	private static void setLinked(ItemStack stack) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(LINKED_KEY, true));
	}
}
