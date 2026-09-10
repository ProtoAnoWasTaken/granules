package com.puppy.granules.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.puppy.granules.fletching.FletchingMenu;

import java.util.List;

public class FletchingScreen extends ItemCombinerScreen<FletchingMenu> {
	private static final Identifier MENU_TEXTURE = Identifier.fromNamespaceAndPath("granules", "textures/gui/sprites/container/fletching.png");
	private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/smithing/error");
	private static final Identifier SHAFT_SLOT_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/slot/stick");
	private static final Identifier FLETCH_SLOT_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/slot/feather");
	private static final Identifier HEAD_SLOT_SPRITE = Identifier.fromNamespaceAndPath("granules", "container/slot/flint");
	private final CyclingSlotBackground shaftIcon = new CyclingSlotBackground(FletchingMenu.SHAFT_SLOT);
	private final CyclingSlotBackground fletchIcon = new CyclingSlotBackground(FletchingMenu.FLETCH_SLOT);
	private final CyclingSlotBackground headIcon = new CyclingSlotBackground(FletchingMenu.HEAD_SLOT);

	public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, MENU_TEXTURE);
		this.titleLabelX = 44;
		this.titleLabelY = 15;
	}

	@Override
	public void containerTick() {
		super.containerTick();
		this.shaftIcon.tick(List.of(SHAFT_SLOT_SPRITE));
		this.fletchIcon.tick(List.of(FLETCH_SLOT_SPRITE));
		this.headIcon.tick(List.of(HEAD_SLOT_SPRITE));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		this.shaftIcon.extractRenderState(this.menu, graphics, delta, this.leftPos, this.topPos);
		this.fletchIcon.extractRenderState(this.menu, graphics, delta, this.leftPos, this.topPos);
		this.headIcon.extractRenderState(this.menu, graphics, delta, this.leftPos, this.topPos);
	}

	@Override
	protected void extractErrorIcon(GuiGraphicsExtractor graphics, int left, int top) {
		if (this.menu.hasRecipeError()) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, left + 65, top + 46, 28, 21);
		}
	}
}
