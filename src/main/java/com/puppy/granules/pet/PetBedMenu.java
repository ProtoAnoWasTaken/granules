package com.puppy.granules.pet;

import com.puppy.granules.GranulesMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PetBedMenu extends AbstractContainerMenu {
	public static final int PET_SLOT_COUNT = 12;
	private final ContainerLevelAccess access;
	private final SimpleContainer petSlots = new SimpleContainer(PET_SLOT_COUNT);
	private final List<PetBedService.PetEntry> entries = new ArrayList<>();
	private final DataSlot petCount = DataSlot.standalone();
	private final DataSlot pageNumber = DataSlot.standalone();
	private int page;
	private UUID pendingDelist;
	private long pendingDelistUntil;

	public PetBedMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public PetBedMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(GranulesMod.PET_BED_MENU, containerId);
		this.access = access;
		for (int slot = 0; slot < PET_SLOT_COUNT; slot++) {
			int x = 44 + slot % 4 * 18;
			int y = 17 + slot / 4 * 18;
			addSlot(new PetSlot(petSlots, slot, x, y));
		}
		addDataSlot(petCount);
		addDataSlot(pageNumber);
		addStandardInventorySlots(inventory, 8, 84);
		if (inventory.player instanceof ServerPlayer serverPlayer) {
			refresh(serverPlayer);
		}
	}

	public static MenuProvider provider(Level level, BlockPos pos) {
		return new SimpleMenuProvider(
			(containerId, inventory, player) -> new PetBedMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
			Component.translatable("container.granules.pet_bed")
		);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, GranulesMod.PET_BED);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < PET_SLOT_COUNT && player instanceof ServerPlayer serverPlayer) {
			PetBedService.PetEntry entry = entryAt(slotIndex);
			if (entry != null) {
				access.execute((level, pos) -> {
					if (level instanceof ServerLevel serverLevel) {
						if (button == 1) {
							long gameTime = serverLevel.getGameTime();
							if (entry.pet().equals(pendingDelist) && gameTime <= pendingDelistUntil) {
								PetBedService.delist(serverPlayer, entry);
								pendingDelist = null;
								pendingDelistUntil = 0L;
							} else {
								pendingDelist = entry.pet();
								pendingDelistUntil = gameTime + 30L;
								serverPlayer.sendSystemMessage(
									Component.translatable("block.granules.pet_bed.confirm_delist", entry.name()),
									true
								);
							}
						} else {
							pendingDelist = null;
							pendingDelistUntil = 0L;
							PetBedService.activate(serverPlayer, serverLevel, pos, entry);
						}
						refresh(serverPlayer);
					}
				});
			}
			return;
		}
		super.clicked(slotIndex, button, input, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int button) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (button == 0 && page > 0) {
			page--;
			refresh(serverPlayer);
			return true;
		}
		if (button == 1 && (page + 1) * PET_SLOT_COUNT < PetBedService.entriesFor(serverPlayer).size()) {
			page++;
			refresh(serverPlayer);
			return true;
		}
		return false;
	}

	public boolean hasScrollbar() {
		return petCount.get() > PET_SLOT_COUNT;
	}

	public int page() {
		return pageNumber.get();
	}

	public int pageCount() {
		return Math.max(1, (petCount.get() + PET_SLOT_COUNT - 1) / PET_SLOT_COUNT);
	}

	private void refresh(ServerPlayer player) {
		entries.clear();
		entries.addAll(PetBedService.entriesFor(player));
		int pageCount = Math.max(1, (entries.size() + PET_SLOT_COUNT - 1) / PET_SLOT_COUNT);
		page = Math.min(page, pageCount - 1);
		petCount.set(entries.size());
		pageNumber.set(page);
		int firstEntry = page * PET_SLOT_COUNT;
		for (int slot = 0; slot < PET_SLOT_COUNT; slot++) {
			int entryIndex = firstEntry + slot;
			ItemStack stack = entryIndex < entries.size()
				? PetBedService.iconFor(entries.get(entryIndex))
				: ItemStack.EMPTY;
			petSlots.setItem(slot, stack);
		}
		broadcastChanges();
	}

	private PetBedService.PetEntry entryAt(int slot) {
		int entryIndex = page * PET_SLOT_COUNT + slot;
		if (entryIndex < 0 || entryIndex >= entries.size()) {
			return null;
		}
		return entries.get(entryIndex);
	}

	private static class PetSlot extends Slot {
		PetSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
