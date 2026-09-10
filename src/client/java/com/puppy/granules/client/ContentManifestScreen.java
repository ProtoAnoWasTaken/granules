package com.puppy.granules.client;

import com.puppy.granules.config.ContentManifest;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class ContentManifestScreen extends Screen {
	private final Screen parent;
	private final HeaderAndFooterLayout layout;
	private final EnumSet<ContentManifest.Category> banned;
	private final Map<ContentManifest.Category, Button> buttons;

	public ContentManifestScreen(Screen parent) {
		super(Component.translatable("granules.config.content.title"));
		this.parent = parent;
		this.layout = new HeaderAndFooterLayout(this, 48, 40);
		this.banned = ContentManifest.get().bannedCategories().isEmpty()
			? EnumSet.noneOf(ContentManifest.Category.class)
			: EnumSet.copyOf(ContentManifest.get().bannedCategories());
		this.buttons = new EnumMap<>(ContentManifest.Category.class);
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.getTitle(), this.font));
		header.addChild(new StringWidget(Component.translatable("granules.config.content.restart"), this.font));
		LinearLayout contents = this.layout.addToContents(LinearLayout.vertical().spacing(4));
		contents.defaultCellSetting().alignHorizontallyCenter();
		for (ContentManifest.Category category : ContentManifest.Category.values()) {
			Button button = Button.builder(this.label(category), pressed -> this.toggle(category)).width(300).build();
			this.buttons.put(category, button);
			contents.addChild(button);
		}
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.save()).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		for (ContentManifest.Category category : ContentManifest.Category.values()) {
			Button button = this.buttons.get(category);
			if (button != null) {
				graphics.item(ContentManifestIcons.icon(category), button.getX() + 6, button.getY() + 2);
			}
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private void toggle(ContentManifest.Category category) {
		if (!this.banned.add(category)) {
			this.banned.remove(category);
		}
		this.buttons.get(category).setMessage(this.label(category));
	}

	private Component label(ContentManifest.Category category) {
		Component state = this.banned.contains(category)
			? Component.translatable("granules.config.content.banned").copy().withStyle(ChatFormatting.RED)
			: Component.translatable("granules.config.content.enabled").copy().withStyle(ChatFormatting.GREEN);
		return Component.translatable(category.translationKey(), state);
	}

	private void save() {
		ContentManifest.replace(this.banned);
		this.onClose();
	}
}
