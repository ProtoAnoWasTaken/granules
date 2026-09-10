package com.puppy.granules.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class GranulesConfigScreen extends Screen {
	private final Screen parent;
	private boolean chunkLoadPriority;

	public GranulesConfigScreen(Screen parent) {
		super(Component.translatable("granules.config.title"));
		this.parent = parent;
		this.chunkLoadPriority = GranulesConfig.load().chunkLoadPriority();
	}

	@Override
	protected void init() {
		StringWidget title = new StringWidget(this.getTitle(), this.font);
		title.setPosition(this.width / 2 - 100, 40);
		this.addRenderableWidget(title);
		StringWidget savedImmediately = new StringWidget(
			Component.translatable("granules.config.applies_immediately"),
			this.font
		);
		savedImmediately.setPosition(this.width / 2 - 100, 64);
		this.addRenderableWidget(savedImmediately);
		this.addRenderableWidget(Button.builder(this.chunkLoadPriorityLabel(), button -> {
			this.chunkLoadPriority = !this.chunkLoadPriority;
			button.setMessage(this.chunkLoadPriorityLabel());
		}).bounds(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build());
		Button contentManifest = Button.builder(
			Component.translatable("granules.config.content.button"),
			button -> this.minecraft.gui.setScreen(new ContentManifestScreen(this))
		).bounds(this.width / 2 - 100, this.height / 2 + 16, 200, 20).build();
		contentManifest.active = this.minecraft.level == null;
		this.addRenderableWidget(contentManifest);
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			GranulesConfig.save(new GranulesConfig(this.chunkLoadPriority));
			ChunkLoadPerformanceController.setEnabled(this.minecraft, this.chunkLoadPriority);
			this.onClose();
		}).bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	private Component chunkLoadPriorityLabel() {
		return Component.translatable(
			"granules.config.chunk_load_priority",
			this.chunkLoadPriority
				? Component.translatable("options.on")
				: Component.translatable("options.off")
		);
	}
}
