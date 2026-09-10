package com.puppy.granules.client;

import com.puppy.granules.pet.PetBedMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class PetBedScreen extends AbstractContainerScreen<PetBedMenu> {
	private static final Identifier MENU_TEXTURE = Identifier.fromNamespaceAndPath("granules", "textures/gui/pet_bed.png");
	private static final int SCROLLBAR_X = 120;
	private static final int SCROLLBAR_Y = 17;
	private static final int SCROLLBAR_HEIGHT = 52;
	private static final int SCROLLBAR_HANDLE_HEIGHT = 15;

	public PetBedScreen(PetBedMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 166);
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 73;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			MENU_TEXTURE,
			this.leftPos,
			this.topPos,
			0.0F,
			0.0F,
			this.imageWidth,
			this.imageHeight,
			256,
			256
		);
		if (menu.hasScrollbar()) {
			int availableHeight = SCROLLBAR_HEIGHT - SCROLLBAR_HANDLE_HEIGHT;
			int maximumPage = menu.pageCount() - 1;
			int handleOffset = maximumPage == 0 ? 0 : availableHeight * menu.page() / maximumPage;
			graphics.fill(
				this.leftPos + SCROLLBAR_X,
				this.topPos + SCROLLBAR_Y + handleOffset,
				this.leftPos + SCROLLBAR_X + 8,
				this.topPos + SCROLLBAR_Y + handleOffset + SCROLLBAR_HANDLE_HEIGHT,
				0xFFC6C6C6
			);
			graphics.outline(
				this.leftPos + SCROLLBAR_X,
				this.topPos + SCROLLBAR_Y + handleOffset,
				8,
				SCROLLBAR_HANDLE_HEIGHT,
				0xFF555555
			);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount == 0.0D || this.minecraft.gameMode == null) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, verticalAmount > 0.0D ? 0 : 1);
		return true;
	}
}
