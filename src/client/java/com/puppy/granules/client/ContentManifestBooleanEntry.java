package com.puppy.granules.client;

import com.puppy.granules.config.ContentManifest;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ContentManifestBooleanEntry extends BooleanListEntry {
	private final ContentManifest.Category category;

	public ContentManifestBooleanEntry(
		ContentManifest.Category category,
		boolean enabled,
		Consumer<Boolean> saveConsumer
	) {
		super(
			Component.translatable(category.translationKey(), Component.empty()),
			enabled,
			Component.translatable("text.cloth-config.reset_value"),
			() -> true,
			saveConsumer
		);
		this.category = category;
	}

	@Override
	public void extractRenderState(
		GuiGraphicsExtractor graphics,
		int index,
		int y,
		int x,
		int entryWidth,
		int entryHeight,
		int mouseX,
		int mouseY,
		boolean hovered,
		float delta
	) {
		int iconSpace = 22;
		super.extractRenderState(
			graphics,
			index,
			y,
			x + iconSpace,
			entryWidth - iconSpace,
			entryHeight,
			mouseX,
			mouseY,
			hovered,
			delta
		);
		graphics.item(ContentManifestIcons.icon(this.category), x + 2, y + (entryHeight - 16) / 2);
	}
}
