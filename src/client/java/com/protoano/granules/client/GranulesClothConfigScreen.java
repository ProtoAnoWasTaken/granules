package com.protoano.granules.client;

import com.protoano.granules.config.MoverConfig;
import com.protoano.granules.config.ContentManifest;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;

public final class GranulesClothConfigScreen {
	private GranulesClothConfigScreen() {
	}

	public static Screen create(Screen parent) {
		GranulesConfig config = GranulesConfig.load();
		MoverConfig moverConfig = MoverConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("granules.config.title"));
		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("granules.config.title"));
		ConfigEntryBuilder entries = builder.entryBuilder();
		general.addEntry(entries.startBooleanToggle(
			Component.translatable("granules.config.chunk_load_priority"),
			config.chunkLoadPriority()
		).setDefaultValue(true).setTooltip(
			Component.translatable("granules.config.chunk_load_priority.tooltip")
		).setSaveConsumer(chunkLoadPriority -> {
			GranulesConfig.save(new GranulesConfig(chunkLoadPriority));
			ChunkLoadPerformanceController.setEnabled(Minecraft.getInstance(), chunkLoadPriority);
		}).build());
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().hasSingleplayerServer()) {
            general.addEntry(entries.startIntField(
                Component.translatable("granules.config.bell_summon_radius"),
                moverConfig.bellSummonRadius()
            ).setDefaultValue(MoverConfig.DEFAULT_BELL_SUMMON_RADIUS).setMin(
                MoverConfig.MIN_BELL_SUMMON_RADIUS
            ).setMax(MoverConfig.MAX_BELL_SUMMON_RADIUS).setTooltip(
                Component.translatable("granules.config.bell_summon_radius.tooltip")
            ).setSaveConsumer(bellSummonRadius -> MoverConfig.update(
                bellSummonRadius,
                MoverConfig.get().moverPullDistance()
            )).build());
            general.addEntry(entries.startIntField(
                Component.translatable("granules.config.mover_pull_distance"),
                moverConfig.moverPullDistance()
            ).setDefaultValue(MoverConfig.DEFAULT_MOVER_PULL_DISTANCE).setMin(
                MoverConfig.MIN_MOVER_PULL_DISTANCE
            ).setMax(MoverConfig.MAX_MOVER_PULL_DISTANCE).setTooltip(
                Component.translatable("granules.config.mover_pull_distance.tooltip")
            ).setSaveConsumer(moverPullDistance -> MoverConfig.update(
                MoverConfig.get().bellSummonRadius(),
                moverPullDistance
            )).build());
            for (var setting : com.protoano.granules.config.BalanceConfig.Setting.values()) {
                general.addEntry(entries.startDoubleField(Component.translatable("granules.config.balance." + setting.key()), setting.value())
                    .setDefaultValue(setting.defaultValue).setMin(setting.minimum).setMax(setting.maximum)
                    .setTooltip(Component.translatable("granules.config.balance.tooltip"))
                    .setSaveConsumer(value -> com.protoano.granules.config.BalanceConfig.update(setting, value))
                    .requireRestart().build());
            }
        }
		if (Minecraft.getInstance().level == null) {
			EnumSet<ContentManifest.Category> banned = ContentManifest.get().bannedCategories().isEmpty()
				? EnumSet.noneOf(ContentManifest.Category.class)
				: EnumSet.copyOf(ContentManifest.get().bannedCategories());
			for (ContentManifest.Category category : ContentManifest.Category.values()) {
				general.addEntry(new ContentManifestBooleanEntry(category, !banned.contains(category), enabled -> {
					if (enabled) {
						banned.remove(category);
					} else {
						banned.add(category);
					}
					ContentManifest.replace(banned);
				}));
			}
		}
		return builder.build();
	}
}
