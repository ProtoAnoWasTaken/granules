package com.puppy.granules.client;

import com.ibm.icu.text.Collator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MultibiomeSelectionScreen extends Screen {
	private static final Component SEARCH_HINT = Component.translatable("createWorld.customize.buffet.search")
		.withStyle(EditBox.SEARCH_HINT_STYLE);
	private final HeaderAndFooterLayout layout;
	private final CreateWorldScreen parent;
	private final Registry<Biome> biomes;
	private final Set<Holder<Biome>> selectedBiomes;
	private BiomeList list;
	private Button doneButton;

	public MultibiomeSelectionScreen(CreateWorldScreen parent, WorldCreationContext context) {
		super(Component.translatable("granules.multibiome.title"));
		this.parent = parent;
		this.layout = new HeaderAndFooterLayout(this, 73, 33);
		this.biomes = context.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
		this.selectedBiomes = new LinkedHashSet<>(context.selectedDimensions().overworld().getBiomeSource().possibleBiomes());
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	protected void init() {
		LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(3));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(this.getTitle(), this.font));
		EditBox searchBox = header.addChild(new EditBox(this.font, 200, 15, Component.empty()));
		this.list = this.layout.addToContents(new BiomeList());
		searchBox.setHint(SEARCH_HINT);
		searchBox.setResponder(this.list::filterEntries);

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
			GranulesClient.applySelectedBiomes(this.parent, new ArrayList<>(this.selectedBiomes));
			this.onClose();
		}).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
		this.updateButtonValidity();
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		this.list.updateSize(this.width, this.layout);
	}

	private void toggleBiome(Holder<Biome> biome) {
		if (!this.selectedBiomes.add(biome)) {
			this.selectedBiomes.remove(biome);
		}

		this.updateButtonValidity();
	}

	private void updateButtonValidity() {
		this.doneButton.active = !this.selectedBiomes.isEmpty();
	}

	private class BiomeList extends ObjectSelectionList<BiomeList.Entry> {
		private BiomeList() {
			super(
				MultibiomeSelectionScreen.this.minecraft,
				MultibiomeSelectionScreen.this.width,
				MultibiomeSelectionScreen.this.layout.getContentHeight(),
				MultibiomeSelectionScreen.this.layout.getHeaderHeight(),
				15
			);
			this.filterEntries("");
		}

		private void filterEntries(String query) {
			Collator collator = Collator.getInstance(Locale.getDefault());
			String normalizedQuery = query.toLowerCase(Locale.ROOT);
			List<Entry> entries = MultibiomeSelectionScreen.this.biomes.listElements()
				.map(Entry::new)
				.sorted(java.util.Comparator.comparing(entry -> entry.name.getString(), collator))
				.filter(entry -> normalizedQuery.isEmpty() || entry.name.getString().toLowerCase(Locale.ROOT).contains(normalizedQuery))
				.toList();
			this.replaceEntries(entries);
			this.refreshScrollAmount();
		}

		private class Entry extends ObjectSelectionList.Entry<Entry> {
			private final Holder.Reference<Biome> biome;
			private final Component name;

			private Entry(Holder.Reference<Biome> biome) {
				this.biome = biome;
				Identifier identifier = biome.key().identifier();
				String translationKey = identifier.toLanguageKey("biome");
				this.name = Language.getInstance().has(translationKey)
					? Component.translatable(translationKey)
					: Component.literal(identifier.toString());
			}

			public Component getNarration() {
				return Component.translatable("narrator.select", this.name);
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
				MutableComponent label = MultibiomeSelectionScreen.this.selectedBiomes.contains(this.biome)
					? Component.literal("[x] ")
					: Component.literal("[ ] ");
				label.append(this.name);
				graphics.text(
					MultibiomeSelectionScreen.this.font,
					label,
					this.getContentX() + 5,
					this.getContentY() + 2,
					-1
				);
			}

			@Override
			public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
				MultibiomeSelectionScreen.this.toggleBiome(this.biome);
				return super.mouseClicked(event, doubleClick);
			}
		}
	}
}
