package com.puppy.granules.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PurgeDimensionsScreen extends Screen {
	private final Screen parent;
	private final LevelStorageSource.LevelStorageAccess levelAccess;
	private final HeaderAndFooterLayout layout;
	private final Set<String> selected;
	private List<DimensionEntry> dimensions;
	private DimensionList list;
	private String armed;
	private Component error;

	public PurgeDimensionsScreen(Screen parent, LevelStorageSource.LevelStorageAccess levelAccess) {
		super(Component.translatable("granules.purge_dimensions.title"));
		this.parent = parent;
		this.levelAccess = levelAccess;
		this.layout = new HeaderAndFooterLayout(this, 36, 40);
		this.selected = new HashSet<>();
		this.dimensions = this.scanDimensions();
	}

	@Override
	protected void init() {
		this.layout.addTitleHeader(this.getTitle(), this.font);
		this.list = this.layout.addToContents(new DimensionList());
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("granules.purge_dimensions.save"), button -> this.save()).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		this.list.updateSize(this.width, this.layout);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		if (this.error != null) {
			graphics.centeredText(this.font, this.error, this.width / 2, this.height - 62, 0xFFFF5555);
		}
	}

	private void activate(DimensionEntry entry) {
		if (!entry.reasons().isEmpty()) {
			return;
		}
		if (this.selected.remove(entry.id())) {
			this.armed = null;
			return;
		}
		if (entry.id().equals(this.armed)) {
			this.selected.add(entry.id());
			this.armed = null;
			return;
		}
		this.armed = entry.id();
	}

	private void save() {
		try {
			for (DimensionEntry entry : this.dimensions) {
				if (this.selected.contains(entry.id())) {
					this.purge(entry);
				}
			}
			this.onClose();
		} catch (IOException exception) {
			this.error = Component.translatable("granules.purge_dimensions.failed", exception.getMessage());
		}
	}

	private void purge(DimensionEntry entry) throws IOException {
		Path root = this.levelAccess.getLevelPath(LevelResource.ROOT).toAbsolutePath().normalize();
		Path target = entry.path().toAbsolutePath().normalize();
		if (!target.startsWith(root)) {
			throw new IOException("Dimension path is outside the selected save");
		}
		if (target.equals(root)) {
			this.deleteTree(root.resolve("region"), root);
			this.deleteTree(root.resolve("entities"), root);
			this.deleteTree(root.resolve("poi"), root);
			return;
		}
		this.deleteTree(target, root);
	}

	private void deleteTree(Path target, Path root) throws IOException {
		Path normalized = target.toAbsolutePath().normalize();
		if (!normalized.startsWith(root) || normalized.equals(root) || !Files.exists(normalized)) {
			return;
		}
		Files.walkFileTree(normalized, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
				if (exception != null) {
					throw exception;
				}
				Files.delete(directory);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private List<DimensionEntry> scanDimensions() {
		Path root = this.levelAccess.getLevelPath(LevelResource.ROOT);
		Map<String, Path> paths = new LinkedHashMap<>();
		Path modernDimensions = root.resolve("dimensions");
		Path modernOverworld = modernDimensions.resolve("minecraft").resolve("overworld");
		if (Files.isDirectory(modernOverworld)) {
			paths.put("minecraft:overworld", modernOverworld);
		} else {
			paths.put("minecraft:overworld", root);
		}
		this.addIfPresent(paths, "minecraft:the_nether", modernDimensions.resolve("minecraft").resolve("the_nether"));
		this.addIfPresent(paths, "minecraft:the_end", modernDimensions.resolve("minecraft").resolve("the_end"));
		if (!paths.containsKey("minecraft:the_nether")) {
			this.addIfPresent(paths, "minecraft:the_nether", root.resolve("DIM-1"));
		}
		if (!paths.containsKey("minecraft:the_end")) {
			this.addIfPresent(paths, "minecraft:the_end", root.resolve("DIM1"));
		}
		Path customRoot = root.resolve("dimensions");
		if (Files.isDirectory(customRoot)) {
			try (DirectoryStream<Path> namespaces = Files.newDirectoryStream(customRoot)) {
				for (Path namespace : namespaces) {
					if (!Files.isDirectory(namespace)) {
						continue;
					}
					try (var dimensionPaths = Files.walk(namespace)) {
						dimensionPaths.filter(Files::isDirectory).forEach(path -> {
							if (Files.isDirectory(path.resolve("region"))) {
								String relative = namespace.relativize(path).toString().replace('\\', '/');
								paths.put(namespace.getFileName() + ":" + relative, path);
							}
						});
					}
				}
			} catch (IOException exception) {
				this.error = Component.translatable("granules.purge_dimensions.scan_failed");
			}
		}
		Map<String, List<Component>> blockers = this.scanPlayers(root);
		List<DimensionEntry> result = new ArrayList<>();
		for (Map.Entry<String, Path> path : paths.entrySet()) {
			List<Component> reasons = new ArrayList<>(blockers.getOrDefault(path.getKey(), List.of()));
			if (!this.hasChunkData(path.getValue())) {
				reasons.add(Component.translatable("granules.purge_dimensions.no_chunks", this.dimensionName(path.getKey())));
			}
			result.add(new DimensionEntry(path.getKey(), path.getValue(), this.dimensionName(path.getKey()), reasons));
		}
		result.sort(Comparator.comparing(entry -> entry.name().getString()));
		return result;
	}

	private void addIfPresent(Map<String, Path> paths, String id, Path path) {
		if (Files.isDirectory(path)) {
			paths.put(id, path);
		}
	}

	private boolean hasChunkData(Path path) {
		Path region = path.resolve("region");
		if (!Files.isDirectory(region)) {
			return false;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(region, "*.mca")) {
			return files.iterator().hasNext();
		} catch (IOException exception) {
			return false;
		}
	}

	private Map<String, List<Component>> scanPlayers(Path root) {
		Map<String, List<Component>> blockers = new HashMap<>();
		Map<UUID, String> names = this.loadPlayerNames();
		Set<UUID> scannedPlayers = new HashSet<>();
		Path playerData = this.levelAccess.getLevelPath(LevelResource.PLAYER_DATA_DIR);
		if (Files.isDirectory(playerData)) {
			try (DirectoryStream<Path> files = Files.newDirectoryStream(playerData, "*.dat")) {
				for (Path file : files) {
					String filename = file.getFileName().toString();
					UUID uuid;
					try {
						uuid = UUID.fromString(filename.substring(0, filename.length() - 4));
					} catch (IllegalArgumentException exception) {
						continue;
					}
					String name = names.getOrDefault(uuid, uuid.toString());
					CompoundTag player = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
					this.scanPlayerTag(blockers, player, name);
					scannedPlayers.add(uuid);
				}
			} catch (IOException exception) {
				this.error = Component.translatable("granules.purge_dimensions.player_scan_failed");
			}
		}
		UUID localPlayer = this.minecraft.getUser().getProfileId();
		if (!scannedPlayers.contains(localPlayer)) {
			try {
				CompoundTag levelData = NbtIo.readCompressed(root.resolve("level.dat"), NbtAccounter.unlimitedHeap());
				CompoundTag player = levelData.getCompoundOrEmpty("Data").getCompoundOrEmpty("Player");
				if (!player.isEmpty()) {
					this.scanPlayerTag(blockers, player, this.minecraft.getUser().getName());
				}
			} catch (IOException exception) {
				this.error = Component.translatable("granules.purge_dimensions.player_scan_failed");
			}
		}
		return blockers;
	}

	private void scanPlayerTag(Map<String, List<Component>> blockers, CompoundTag player, String name) {
		this.addPlayerBlocker(blockers, player.getStringOr("Dimension", "minecraft:overworld"), name, "granules.purge_dimensions.current_location");
		if (player.contains("respawn")) {
			CompoundTag respawn = player.getCompoundOrEmpty("respawn");
			this.addPlayerBlocker(blockers, respawn.getStringOr("dimension", "minecraft:overworld"), name, "granules.purge_dimensions.spawn_location");
		} else if (player.contains("SpawnX")) {
			this.addPlayerBlocker(blockers, player.getStringOr("SpawnDimension", "minecraft:overworld"), name, "granules.purge_dimensions.spawn_location");
		}
	}

	private void addPlayerBlocker(Map<String, List<Component>> blockers, String dimension, String name, String reasonKey) {
		blockers.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(
			Component.translatable(reasonKey, name, this.dimensionName(dimension))
		);
	}

	private Map<UUID, String> loadPlayerNames() {
		Map<UUID, String> names = new HashMap<>();
		names.put(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getName());
		Path cache = this.minecraft.gameDirectory.toPath().resolve("usercache.json");
		if (!Files.isRegularFile(cache)) {
			return names;
		}
		try (Reader reader = Files.newBufferedReader(cache)) {
			for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
				JsonObject entry = element.getAsJsonObject();
				names.put(UUID.fromString(entry.get("uuid").getAsString()), entry.get("name").getAsString());
			}
		} catch (Exception exception) {
			return names;
		}
		return names;
	}

	private Component dimensionName(String id) {
		return switch (id) {
			case "minecraft:overworld" -> Component.translatable("granules.purge_dimensions.overworld");
			case "minecraft:the_nether" -> Component.translatable("granules.purge_dimensions.nether");
			case "minecraft:the_end" -> Component.translatable("granules.purge_dimensions.end");
			default -> this.customDimensionName(id);
		};
	}

	private Component customDimensionName(String id) {
		Identifier identifier = Identifier.tryParse(id);
		if (identifier == null) {
			return Component.literal(id);
		}
		String translationKey = identifier.toLanguageKey("dimension");
		if (Language.getInstance().has(translationKey)) {
			return Component.translatable(translationKey);
		}
		return Component.literal(id);
	}

	private record DimensionEntry(String id, Path path, Component name, List<Component> reasons) {
	}

	private final class DimensionList extends ObjectSelectionList<DimensionList.Entry> {
		private DimensionList() {
			super(PurgeDimensionsScreen.this.minecraft, PurgeDimensionsScreen.this.width, PurgeDimensionsScreen.this.layout.getContentHeight(), PurgeDimensionsScreen.this.layout.getHeaderHeight(), 34);
			this.replaceEntries(PurgeDimensionsScreen.this.dimensions.stream().map(Entry::new).toList());
		}

		@Override
		public int getRowWidth() {
			return Math.min(this.width - 40, 600);
		}

		private final class Entry extends ObjectSelectionList.Entry<Entry> {
			private final DimensionEntry dimension;
			private final Button button;

			private Entry(DimensionEntry dimension) {
				this.dimension = dimension;
				this.button = Button.builder(this.label(), pressed -> PurgeDimensionsScreen.this.activate(this.dimension))
					.size(200, 30)
					.build();
				this.button.active = this.dimension.reasons().isEmpty();
				if (!this.dimension.reasons().isEmpty()) {
					this.button.setTooltip(Tooltip.create(this.tooltip()));
				}
			}

			@Override
			public Component getNarration() {
				return this.label();
			}

			@Override
			public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
				int left = this.getContentX() + 10;
				int right = this.getContentRight() - 10;
				int top = this.getContentY();
				this.button.setRectangle(right - left, 30, left, top);
				this.button.setMessage(this.label());
				this.button.extractRenderState(graphics, mouseX, mouseY, delta);
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
				return this.button.mouseClicked(event, doubleClick);
			}

			private Component tooltip() {
				MutableComponent tooltip = Component.empty();
				for (int index = 0; index < this.dimension.reasons().size(); index++) {
					if (index > 0) {
						tooltip.append("\n");
					}
					tooltip.append(this.dimension.reasons().get(index));
				}
				return tooltip;
			}

			private Component label() {
				if (!this.dimension.reasons().isEmpty()) {
					return this.dimension.name().copy().withStyle(ChatFormatting.DARK_GRAY);
				}
				if (PurgeDimensionsScreen.this.selected.contains(this.dimension.id())) {
					return Component.translatable("granules.purge_dimensions.selected", this.dimension.name()).withStyle(ChatFormatting.RED);
				}
				if (this.dimension.id().equals(PurgeDimensionsScreen.this.armed)) {
					return Component.translatable("granules.purge_dimensions.confirm", this.dimension.name()).withStyle(ChatFormatting.RED);
				}
				return this.dimension.name();
			}

		}
	}
}
