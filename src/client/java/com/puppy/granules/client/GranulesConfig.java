package com.puppy.granules.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public record GranulesConfig(boolean chunkLoadPriority) {
	private static final Logger LOGGER = LoggerFactory.getLogger("Granules Config");
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("granules.json");

	public static GranulesConfig load() {
		if (Files.notExists(CONFIG_PATH)) {
			GranulesConfig defaultConfig = new GranulesConfig(true);
			save(defaultConfig);
			return defaultConfig;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			boolean chunkLoadPriority = !json.has("chunkLoadPriority") || json.get("chunkLoadPriority").getAsBoolean();
			return new GranulesConfig(chunkLoadPriority);
		} catch (IOException | JsonParseException | IllegalStateException exception) {
			LOGGER.warn("Could not read Granules configuration from {}. Using default settings.", CONFIG_PATH, exception);
			return new GranulesConfig(true);
		}
	}

	public static void save(GranulesConfig config) {
		JsonObject json = new JsonObject();
		json.addProperty("chunkLoadPriority", config.chunkLoadPriority());

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				writer.write(json.toString());
			}
		} catch (IOException exception) {
			LOGGER.warn("Could not create Granules configuration at {}.", CONFIG_PATH, exception);
		}
	}
}
