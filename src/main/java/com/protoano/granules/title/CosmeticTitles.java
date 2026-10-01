package com.protoano.granules.title;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CosmeticTitles {
    private static final Map<String, String> TITLES = load();

    private CosmeticTitles() {
    }

    public static Component resolve(Identifier advancement, Component fallback) {
        String title = TITLES.get(advancement.toString());
        if (title == null) {
            return fallback;
        }
        String key = "title.granules." + advancement.getNamespace() + "." + advancement.getPath().replace('/', '.');
        return Component.translatableWithFallback(key, title);
    }

    private static Map<String, String> load() {
        try (var stream = CosmeticTitles.class.getResourceAsStream("/granules/advancement_titles.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing cosmetic title mappings");
            }
            var result = new HashMap<String, String>();
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : json.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return Map.copyOf(result);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load cosmetic title mappings", exception);
        }
    }
}
