package com.protoano.granules.disc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;

public final class KnownMusic {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Granules Music");
    private static List<Holder<JukeboxSong>> cached;

    private KnownMusic() {
    }

    public static synchronized List<Holder<JukeboxSong>> songs() {
        if (cached == null) {
            cached = discover();
        }
        return cached;
    }

    private static List<Holder<JukeboxSong>> discover() {
        Map<Identifier, JsonObject> events = new HashMap<>();
        List<Path> roots = new ArrayList<>();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            for (Path root : mod.getRootPaths()) {
                roots.add(root);
                Path assets = root.resolve("assets");
                if (!Files.isDirectory(assets)) {
                    continue;
                }
                try (var namespaces = Files.list(assets)) {
                    for (Path namespace : namespaces.toList()) {
                        Path sounds = namespace.resolve("sounds.json");
                        if (!Files.isRegularFile(sounds)) {
                            continue;
                        }
                        try (var reader = Files.newBufferedReader(sounds)) {
                            for (var entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
                                Identifier id = Identifier.tryParse(namespace.getFileName() + ":" + entry.getKey());
                                if (id != null && entry.getValue().isJsonObject()) {
                                    events.put(id, entry.getValue().getAsJsonObject());
                                }
                            }
                        }
                    }
                } catch (IOException | RuntimeException exception) {
                    LOGGER.debug("Cannot inspect music assets for {}", mod.getMetadata().getId(), exception);
                }
            }
        }
        Set<String> bundled = bundledEvents();
        var result = new ArrayList<Holder<JukeboxSong>>();
        var includedFiles = new HashSet<Identifier>();
        for (var id : events.keySet().stream().sorted().toList()) {
            if (id.getNamespace().equals("granules") || bundled.contains(id.toString())) {
                continue;
            }
            Identifier file;
            try {
                file = resolveFile(id, events, new HashSet<>());
            } catch (RuntimeException exception) {
                LOGGER.debug("Skipping malformed music event {}", id, exception);
                continue;
            }
            if (file == null || includedFiles.contains(file)) {
                continue;
            }
            if (!isMusic(id) && !file.getPath().startsWith("music/") && !file.getPath().contains("/music/")) {
                continue;
            }
            float seconds = duration(file, roots);
            if (!(seconds > 0) || !Float.isFinite(seconds)) {
                continue;
            }
            includedFiles.add(file);
            String name = file.getPath().substring(file.getPath().lastIndexOf('/') + 1).replace('_', ' ');
            result.add(Holder.direct(new JukeboxSong(Holder.direct(SoundEvent.createVariableRangeEvent(id)),
                Component.literal(name), seconds, 1)));
        }
        return List.copyOf(result);
    }

    public static boolean isMusic(Identifier id) {
        String path = id.getPath();
        return path.equals("music") || path.startsWith("music.") || path.startsWith("music/")
            || path.contains(".music.") || path.contains("/music/");
    }

    private static Set<String> bundledEvents() {
        try (var stream = KnownMusic.class.getResourceAsStream("/data/granules/bundled_music_events.txt")) {
            if (stream == null) {
                return Set.of();
            }
            return Set.copyOf(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList());
        } catch (IOException exception) {
            return Set.of();
        }
    }

    private static Identifier resolveFile(Identifier event, Map<Identifier, JsonObject> events, Set<Identifier> visiting) {
        if (!visiting.add(event)) {
            return null;
        }
        JsonObject definition = events.get(event);
        if (definition == null || !definition.has("sounds") || !definition.get("sounds").isJsonArray()) {
            return null;
        }
        var sounds = definition.getAsJsonArray("sounds");
        if (sounds.size() != 1) {
            return null;
        }
        JsonElement sound = sounds.get(0);
        String name;
        boolean reference = false;
        if (sound.isJsonPrimitive() && sound.getAsJsonPrimitive().isString()) {
            name = sound.getAsString();
        } else if (sound.isJsonObject()) {
            var object = sound.getAsJsonObject();
            if (!object.has("name") || object.has("pitch") && object.get("pitch").getAsFloat() != 1) {
                return null;
            }
            name = object.get("name").getAsString();
            reference = object.has("type") && object.get("type").getAsString().equals("event");
        } else {
            return null;
        }
        Identifier id = Identifier.tryParse(name);
        if (id == null) {
            return null;
        }
        return reference ? resolveFile(id, events, visiting) : id;
    }

    private static float duration(Identifier file, List<Path> roots) {
        if (file.getPath().contains("..")) {
            return -1;
        }
        for (Path root : roots) {
            Path path = root.resolve("assets").resolve(file.getNamespace()).resolve("sounds").resolve(file.getPath() + ".ogg");
            if (Files.isRegularFile(path)) {
                try (var input = Files.newInputStream(path)) {
                    return vorbisSeconds(input);
                } catch (IOException | RuntimeException exception) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static float vorbisSeconds(InputStream input) throws IOException {
        int sampleRate = 0;
        int serial = 0;
        long samples = -1;
        boolean ended = false;
        while (true) {
            byte[] header = input.readNBytes(27);
            if (header.length == 0) {
                break;
            }
            if (header.length != 27 || header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S' || header[4] != 0) {
                return -1;
            }
            var buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            int pageSerial = buffer.getInt(14);
            byte[] segments = input.readNBytes(Byte.toUnsignedInt(header[26]));
            if (segments.length != Byte.toUnsignedInt(header[26])) {
                return -1;
            }
            int size = 0;
            for (byte segment : segments) {
                size += Byte.toUnsignedInt(segment);
            }
            byte[] body = input.readNBytes(size);
            if (body.length != size) {
                return -1;
            }
            if (sampleRate == 0) {
                if ((header[5] & 2) == 0 || body.length < 16 || body[0] != 1
                    || body[1] != 'v' || body[2] != 'o' || body[3] != 'r' || body[4] != 'b' || body[5] != 'i' || body[6] != 's') {
                    return -1;
                }
                sampleRate = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
                serial = pageSerial;
                if (sampleRate <= 0) {
                    return -1;
                }
            } else if (pageSerial != serial || ended) {
                return -1;
            }
            if (buffer.getLong(6) >= 0) {
                samples = buffer.getLong(6);
            }
            ended = (header[5] & 4) != 0;
        }
        return ended && samples > 0 && sampleRate > 0 ? (float) ((double) samples / sampleRate) : -1;
    }
}
