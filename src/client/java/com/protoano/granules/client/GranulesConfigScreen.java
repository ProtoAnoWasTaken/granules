package com.protoano.granules.client;

import com.protoano.granules.config.BalanceConfig;
import com.protoano.granules.config.ContentManifest;
import com.protoano.granules.config.MoverConfig;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class GranulesConfigScreen extends Screen {
    private final Screen parent;
    private final EnumMap<BalanceConfig.Setting, Double> pending = new EnumMap<>(BalanceConfig.Setting.class);
    private final EnumSet<ContentManifest.Category> banned = EnumSet.noneOf(ContentManifest.Category.class);
    private final Map<String, String> drafts = new HashMap<>();
    private final java.util.Set<String> invalid = new java.util.HashSet<>();
    private boolean chunkLoadPriority = GranulesConfig.load().chunkLoadPriority();
    private int bellSummonRadius = MoverConfig.get().bellSummonRadius();
    private int moverPullDistance = MoverConfig.get().moverPullDistance();
    private int page;
    private Button done;

    public GranulesConfigScreen(Screen parent) {
        super(Component.translatable("granules.config.title"));
        this.parent = parent;
        banned.addAll(ContentManifest.get().bannedCategories());
        for (var setting : BalanceConfig.Setting.values()) {
            pending.put(setting, setting.value());
        }
    }

    @Override
    protected void init() {
        var title = new StringWidget(getTitle(), font);
        title.setPosition(width / 2 - title.getWidth() / 2, 12);
        addRenderableWidget(title);
        var entries = new ArrayList<IntConsumer>();
        entries.add(y -> addToggle("granules.config.chunk_load_priority", chunkLoadPriority, y, value -> chunkLoadPriority = value));
        boolean local = minecraft.level == null || minecraft.hasSingleplayerServer();
        if (local) {
            entries.add(y -> addNumber("granules.config.bell_summon_radius", bellSummonRadius,
                MoverConfig.MIN_BELL_SUMMON_RADIUS, MoverConfig.MAX_BELL_SUMMON_RADIUS, true, y,
                value -> bellSummonRadius = value.intValue()));
            entries.add(y -> addNumber("granules.config.mover_pull_distance", moverPullDistance,
                MoverConfig.MIN_MOVER_PULL_DISTANCE, MoverConfig.MAX_MOVER_PULL_DISTANCE, true, y,
                value -> moverPullDistance = value.intValue()));
            for (var setting : BalanceConfig.Setting.values()) {
                entries.add(y -> addNumber("granules.config.balance." + setting.key(), pending.get(setting),
                    setting.minimum, setting.maximum, false, y, value -> pending.put(setting, value)));
            }
        }
        if (minecraft.level == null) {
            for (var category : ContentManifest.Category.values()) {
                entries.add(y -> addToggle(category.translationKey(), !banned.contains(category), y, enabled -> {
                    if (enabled) {
                        banned.remove(category);
                    } else {
                        banned.add(category);
                    }
                }));
            }
        }
        int rows = Math.max(1, (height - 100) / 28);
        int pages = (entries.size() + rows - 1) / rows;
        page = Math.min(page, pages - 1);
        for (int row = 0; row < rows && page * rows + row < entries.size(); row++) {
            entries.get(page * rows + row).accept(36 + row * 28);
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page--;
            rebuildWidgets();
        }).bounds(width / 2 - 170, height - 32, 35, 20).build());
        previous.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page++;
            rebuildWidgets();
        }).bounds(width / 2 + 135, height - 32, 35, 20).build());
        next.active = page + 1 < pages;
        done = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            GranulesConfig.save(new GranulesConfig(chunkLoadPriority));
            ChunkLoadPerformanceController.setEnabled(minecraft, chunkLoadPriority);
            if (local) {
                MoverConfig.update(bellSummonRadius, moverPullDistance);
                BalanceConfig.updateAll(pending);
            }
            if (minecraft.level == null) {
                ContentManifest.replace(banned);
            }
            onClose();
        }).bounds(width / 2 - 50, height - 32, 100, 20).build());
        done.active = invalid.isEmpty();
        var notice = new StringWidget(Component.translatable("granules.config.balance.restart"), font);
        notice.setPosition(width / 2 - notice.getWidth() / 2, height - 52);
        addRenderableWidget(notice);
    }

    private Component addLabel(String key, int y) {
        var label = Component.translatable(key, Component.empty());
        var widget = new StringWidget(Component.literal(font.plainSubstrByWidth(label.getString(), 230)), font);
        widget.setPosition(width / 2 - 170, y + 6);
        widget.setTooltip(Tooltip.create(label));
        addRenderableWidget(widget);
        return label;
    }

    private void addToggle(String key, boolean value, int y, Consumer<Boolean> consumer) {
        addLabel(key, y);
        boolean[] current = {value};
        addRenderableWidget(Button.builder(Component.translatable(value ? "options.on" : "options.off"), button -> {
            current[0] = !current[0];
            consumer.accept(current[0]);
            button.setMessage(Component.translatable(current[0] ? "options.on" : "options.off"));
        }).bounds(width / 2 + 70, y, 95, 20).build());
    }

    private void addNumber(String key, double value, double minimum, double maximum, boolean integer, int y, Consumer<Double> consumer) {
        var input = new EditBox(font, width / 2 + 70, y, 95, 20, addLabel(key, y));
        input.setValue(drafts.getOrDefault(key, integer ? Integer.toString((int) value) : Double.toString(value)));
        input.setTextColor(invalid.contains(key) ? 0xFFFF5555 : 0xFFE0E0E0);
        input.setResponder(text -> {
            drafts.put(key, text);
            try {
                double parsed = Double.parseDouble(text);
                if (!Double.isFinite(parsed) || parsed < minimum || parsed > maximum || integer && parsed != Math.rint(parsed)) {
                    throw new NumberFormatException();
                }
                invalid.remove(key);
                consumer.accept(parsed);
                input.setTextColor(0xFFE0E0E0);
            } catch (NumberFormatException exception) {
                invalid.add(key);
                input.setTextColor(0xFFFF5555);
            }
            done.active = invalid.isEmpty();
        });
        addRenderableWidget(input);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
