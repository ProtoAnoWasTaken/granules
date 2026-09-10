package com.puppy.granules.compat;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public final class FletchingDisplayCategory implements DisplayCategory<FletchingDisplay> {
	@Override
	public CategoryIdentifier<? extends FletchingDisplay> getCategoryIdentifier() {
		return GranulesReiPlugin.FLETCHING;
	}

	@Override
	public Component getTitle() {
		return Component.literal("Fletching");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(Blocks.FLETCHING_TABLE);
	}

	@Override
	public List<Widget> setupDisplay(FletchingDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		widgets.add(Widgets.createRecipeBase(bounds));
		widgets.add(Widgets.createSlot(new Point(bounds.x + 8, bounds.y + 20)).entries(display.fletch()).markInput());
		widgets.add(Widgets.createSlot(new Point(bounds.x + 30, bounds.y + 20)).entries(display.shaft()).markInput());
		widgets.add(Widgets.createSlot(new Point(bounds.x + 52, bounds.y + 20)).entries(display.head()).markInput());
		widgets.add(Widgets.createArrow(new Point(bounds.x + 78, bounds.y + 21)));
		widgets.add(Widgets.createResultSlotBackground(new Point(bounds.x + 104, bounds.y + 20)));
		widgets.add(Widgets.createSlot(new Point(bounds.x + 104, bounds.y + 20)).entries(display.result()).markOutput());
		return widgets;
	}

	@Override
	public int getDisplayWidth(FletchingDisplay display) {
		return 130;
	}

	@Override
	public int getDisplayHeight() {
		return 58;
	}
}
