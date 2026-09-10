package com.puppy.granules.item;

import com.puppy.granules.advancement.GranulesAdvancements;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.BreadCrumbMarkerAccess;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BreadCrumbsItem extends BlockItem {
	public BreadCrumbsItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		Player player = context.getPlayer();
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer && !canPlaceAt(serverPlayer, context.getClickedPos())) {
			serverPlayer.sendSystemMessage(Component.translatable("item.granules.bread_crumbs.one_trail"), true);
			return InteractionResult.FAIL;
		}
		InteractionResult result = super.place(context);
		if (result.consumesAction() && player instanceof ServerPlayer serverPlayer) {
			BreadCrumbMarkerAccess access = (BreadCrumbMarkerAccess) serverPlayer;
			access.granules$setBreadCrumbMarker(GlobalPos.of(serverPlayer.level().dimension(), context.getClickedPos()));
		}
		return result;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		stack.remove(DataComponents.CONSUMABLE);
		if (level.isClientSide()) {
			player.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !hasValidMarker(serverPlayer)) {
			return InteractionResult.PASS;
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack itemStack, int ticksRemaining) {
		if (level.isClientSide() || !(entity instanceof ServerPlayer player) || itemStack.has(DataComponents.CONSUMABLE)) {
			return;
		}
		if (!hasValidMarker(player)) {
			player.stopUsingItem();
			return;
		}
		if (player.getTicksUsingItem() < 60) {
			return;
		}
		InteractionHand hand = player.getUsedItemHand();
		itemStack.set(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD);
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		if (player.containerMenu != player.inventoryMenu) {
			player.containerMenu.broadcastChanges();
		}
		player.stopUsingItem();
		((BreadCrumbMarkerAccess) player).granules$beginBreadCrumbEating(hand, itemStack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player && !hasValidMarker(player)) {
			itemStack.remove(DataComponents.CONSUMABLE);
			syncInventory(player);
			return itemStack;
		}
		ItemStack result = super.finishUsingItem(itemStack, level, entity);
		result.remove(DataComponents.CONSUMABLE);
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			syncInventory(player);
			teleportToMarker(player);
		}
		return result;
	}

	@Override
	public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
		Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
		return consumable == null ? 72000 : consumable.consumeTicks();
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
		Consumable consumable = itemStack.get(DataComponents.CONSUMABLE);
		return consumable == null ? ItemUseAnimation.BOW : consumable.animation();
	}

	private boolean canPlaceAt(ServerPlayer player, BlockPos placementPos) {
		BreadCrumbMarkerAccess access = (BreadCrumbMarkerAccess) player;
		GlobalPos marker = access.granules$getBreadCrumbMarker();
		if (marker == null) {
			return true;
		}
		if (!marker.dimension().equals(player.level().dimension())) {
			return false;
		}
		if (marker.pos().equals(placementPos)) {
			return true;
		}
		ServerLevel markerLevel = player.level().getServer().getLevel(marker.dimension());
		if (markerLevel != null) {
			markerLevel.getChunkAt(marker.pos());
			if (markerLevel.getBlockState(marker.pos()).is(GranulesMod.BREAD_CRUMBS_BLOCK)) {
				return false;
			}
		}
		access.granules$setBreadCrumbMarker(null);
		return true;
	}

	private boolean hasValidMarker(ServerPlayer player) {
		BreadCrumbMarkerAccess access = (BreadCrumbMarkerAccess) player;
		GlobalPos marker = access.granules$getBreadCrumbMarker();
		if (marker == null) {
			return false;
		}
		ServerLevel markerLevel = player.level().getServer().getLevel(marker.dimension());
		if (markerLevel == null) {
			access.granules$setBreadCrumbMarker(null);
			return false;
		}
		markerLevel.getChunkAt(marker.pos());
		if (markerLevel.getBlockState(marker.pos()).is(GranulesMod.BREAD_CRUMBS_BLOCK)) {
			return true;
		}
		access.granules$setBreadCrumbMarker(null);
		return false;
	}

	private void teleportToMarker(ServerPlayer player) {
		if (!hasValidMarker(player)) {
			return;
		}
		BreadCrumbMarkerAccess access = (BreadCrumbMarkerAccess) player;
		GlobalPos marker = access.granules$getBreadCrumbMarker();
		if (marker == null) {
			return;
		}
		ServerLevel originLevel = player.level();
		ServerLevel markerLevel = originLevel.getServer().getLevel(marker.dimension());
		if (markerLevel == null) {
			access.granules$setBreadCrumbMarker(null);
			return;
		}
		BlockState markerState = markerLevel.getBlockState(marker.pos());
		if (!markerState.is(GranulesMod.BREAD_CRUMBS_BLOCK)) {
			access.granules$setBreadCrumbMarker(null);
			return;
		}
		double originX = player.getX();
		double originY = player.getY();
		double originZ = player.getZ();
		double destinationX = marker.pos().getX() + 0.5D;
		double destinationY = marker.pos().getY() + 0.0625D;
		double destinationZ = marker.pos().getZ() + 0.5D;
		boolean teleported = player.teleportTo(
			markerLevel,
			destinationX,
			destinationY,
			destinationZ,
			Set.of(),
			player.getYRot(),
			player.getXRot(),
			false
		);
		if (!teleported) {
			return;
		}
		originLevel.sendParticles(ParticleTypes.PORTAL, originX, originY + 1.0D, originZ, 32, 0.4D, 0.8D, 0.4D, 0.1D);
		markerLevel.sendParticles(ParticleTypes.PORTAL, destinationX, destinationY + 1.0D, destinationZ, 32, 0.4D, 0.8D, 0.4D, 0.1D);
		originLevel.playSound(null, originX, originY, originZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
		markerLevel.playSound(null, destinationX, destinationY, destinationZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (!originLevel.dimension().equals(marker.dimension())) {
			GranulesAdvancements.award(player, "man_country");
			markerLevel.removeBlock(marker.pos(), false);
			access.granules$setBreadCrumbMarker(null);
			return;
		}
		consumeMarkerSegment(markerLevel, marker.pos(), markerState, access);
	}

	private void consumeMarkerSegment(ServerLevel level, BlockPos markerPos, BlockState markerState, BreadCrumbMarkerAccess access) {
		int segmentAmount = markerState.getValue(SegmentableBlock.AMOUNT);
		if (segmentAmount <= 1) {
			level.removeBlock(markerPos, false);
			access.granules$setBreadCrumbMarker(null);
			return;
		}
		level.setBlock(markerPos, markerState.setValue(SegmentableBlock.AMOUNT, segmentAmount - 1), 3);
	}

	private void syncInventory(ServerPlayer player) {
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
		if (player.containerMenu != player.inventoryMenu) {
			player.containerMenu.broadcastChanges();
		}
	}
}
