package com.protoano.granules.disc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.protoano.granules.network.DiscPalettePayload;
import com.protoano.granules.config.ContentManifest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class DiscContent {
    private static final Palette PALETTE = loadPalette();
    private static final List<String> TRACKS = loadTracks();
    private static volatile BlankColors blankColors;
    public static final Item DISC = Registry.register(BuiltInRegistries.ITEM, id("unmarked_disc"),
        new UnmarkedDiscItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("unmarked_disc"))).stacksTo(1)));
    public static final Block BURNER = Registry.register(BuiltInRegistries.BLOCK, id("disc_burner"),
        new DiscBurnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUKEBOX).setId(ResourceKey.create(Registries.BLOCK, id("disc_burner")))));
    public static final Item BURNER_ITEM = Registry.register(BuiltInRegistries.ITEM, id("disc_burner"),
        new BlockItem(BURNER, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("disc_burner"))).useBlockDescriptionPrefix()));
    public static final BlockEntityType<DiscBurnerBlockEntity> BURNER_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE, id("disc_burner"), new BlockEntityType<>(DiscBurnerBlockEntity::new, Set.of(BURNER)));

    private DiscContent() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("granules", path);
    }

    public static void initialize() {
        boolean banned = ContentManifest.get().isBanned(ContentManifest.Category.UNMARKED_DISCS);
        if (!banned) {
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
                entries.insertAfter(Items.JUKEBOX, BURNER_ITEM);
            });
        }
        PayloadTypeRegistry.clientboundPlay().register(DiscPalettePayload.TYPE, DiscPalettePayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ItemStack blank = new ItemStack(DISC);
            colorBlank(blank, server.overworld());
            List<Integer> colors = blank.get(DataComponents.CUSTOM_MODEL_DATA).colors();
            ServerPlayNetworking.send(handler.getPlayer(), new DiscPalettePayload(colors.get(0), colors.get(1), colors.get(2)));
        });
        for (String track : TRACKS) {
            Identifier sound = id("unmarked_disc." + track);
            Registry.register(BuiltInRegistries.SOUND_EVENT, sound, SoundEvent.createVariableRangeEvent(sound));
        }
        if (!banned) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("unmarked_discs"), FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.granules.unmarked_discs"))
            .icon(() -> new ItemStack(DISC))
            .displayItems((parameters, output) -> {
                for (Item disc : vanillaDiscs()) {
                    output.accept(disc);
                }
                output.accept(DISC);
                for (var song : availableSongs(parameters.holders())) {
                    output.accept(recorded(song, RandomSource.create(song.value().soundEvent().value().location().hashCode())));
                }
                }).build());
        }
    }

    private static List<Item> vanillaDiscs() {
        return BuiltInRegistries.ITEM.stream()
            .filter(item -> item != DISC && item.components().has(DataComponents.JUKEBOX_PLAYABLE)).toList();
    }

    public static List<Holder<JukeboxSong>> availableSongs(net.minecraft.core.HolderLookup.Provider registries) {
        var songs = new java.util.TreeMap<String, Holder<JukeboxSong>>();
        registries.lookupOrThrow(Registries.JUKEBOX_SONG).listElements().forEach(song -> {
            var id = song.key().identifier();
            var sound = song.value().soundEvent().value().location();
            boolean bundled = id.getNamespace().equals("granules") && id.getPath().startsWith("unmarked_disc/");
            if ((bundled || KnownMusic.isMusic(sound)) && song.value().lengthInSeconds() > 0
                && Float.isFinite(song.value().lengthInSeconds())) {
                songs.put(sound.toString(), song);
            }
        });
        for (var song : KnownMusic.songs()) {
            songs.putIfAbsent(song.value().soundEvent().value().location().toString(), song);
        }
        return List.copyOf(songs.values());
    }

    public static boolean isBlank(ItemStack stack) {
        return stack.is(DISC) && !stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    public static void colorBlank(ItemStack stack, ServerLevel level) {
        if (!isBlank(stack)) {
            return;
        }
        long seed = level.getServer().overworld().getSeed();
        BlankColors cached = blankColors;
        if (cached == null || cached.seed() != seed) {
            RandomSource random = RandomSource.create(seed ^ 0x4752414e554c4553L);
            cached = new BlankColors(seed, colors(PALETTE.width() / 2, PALETTE.height() / 2, random));
            blankColors = cached;
        }
        CustomModelData colors = cached.colors();
        if (!colors.equals(stack.get(DataComponents.CUSTOM_MODEL_DATA))) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, colors);
        }
    }

    public static void colorRecorded(ItemStack stack, RandomSource random) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, colors(random.nextInt(PALETTE.width()), random.nextInt(PALETTE.height()), random));
    }

    private static CustomModelData colors(int x, int y, RandomSource random) {
        int bright = PALETTE.at(x, y);
        int middle = adjacent(x, y, -1, random);
        int dark = adjacent(middle % PALETTE.width(), middle / PALETTE.width(), y * PALETTE.width() + x, random);
        int mediumColor = shade(PALETTE.values()[middle], brightness(bright) * 0.72);
        int darkColor = shade(PALETTE.values()[dark], brightness(mediumColor) * 0.65);
        return new CustomModelData(List.of(), List.of(), List.of(), List.of(bright, mediumColor, darkColor));
    }

    private static int shade(int color, double maximumBrightness) {
        int brightness = brightness(color);
        if (brightness <= maximumBrightness || brightness == 0) {
            return color;
        }
        double scale = maximumBrightness / brightness;
        int red = (int)(((color >> 16) & 255) * scale);
        int green = (int)(((color >> 8) & 255) * scale);
        int blue = (int)((color & 255) * scale);
        return (red << 16) | (green << 8) | blue;
    }

    private static int adjacent(int x, int y, int excluded, RandomSource random) {
        List<Integer> candidates = new ArrayList<>();
        List<Integer> darker = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= PALETTE.width() || ny >= PALETTE.height()) {
                    continue;
                }
                int index = ny * PALETTE.width() + nx;
                if (index != excluded) {
                    candidates.add(index);
                    if (brightness(PALETTE.values()[index]) <= brightness(PALETTE.at(x, y))) {
                        darker.add(index);
                    }
                }
            }
        }
        List<Integer> choices = darker.isEmpty() ? candidates : darker;
        return choices.get(random.nextInt(choices.size()));
    }

    private static int brightness(int color) {
        return ((color >> 16) & 255) * 299 + ((color >> 8) & 255) * 587 + (color & 255) * 114;
    }

    public static ItemStack recorded(Holder<JukeboxSong> song, RandomSource random) {
        ItemStack stack = new ItemStack(DISC);
        stack.set(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(song));
        stack.set(DataComponents.RARITY, Rarity.EPIC);
        colorRecorded(stack, random);
        return stack;
    }

    public static void addChestLoot(Container chest, ServerLevel level, long lootSeed, long position) {
        addChestLoot(chest, level, lootSeed, position, 0.0F);
    }

    public static void addChestLoot(Container chest, ServerLevel level, long lootSeed, long position, float luck) {
        RandomSource random = RandomSource.create(level.getSeed() ^ lootSeed ^ position ^ 0x554e4d41524b4544L);
        float roll = random.nextInt(100) + random.nextFloat();
        if (roll >= 100.0F * com.protoano.granules.world.LuckAdjustedChance.adjusted(com.protoano.granules.config.BalanceConfig.Setting.DISC_CHANCE.floatValue(), luck)) {
            return;
        }
        List<Integer> empty = new ArrayList<>();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (chest.getItem(slot).isEmpty()) {
                empty.add(slot);
            }
        }
        if (empty.isEmpty()) {
            return;
        }
        ItemStack disc = randomLootDisc(level, random);
        if (!disc.isEmpty()) {
            chest.setItem(empty.get(random.nextInt(empty.size())), disc);
            chest.setChanged();
        }
    }

    public static ItemStack rollCommandChestLoot(ServerLevel level) {
        return rollCommandChestLoot(level, 0.0F);
    }

    public static ItemStack rollCommandChestLoot(ServerLevel level, float luck) {
        RandomSource random = level.getRandom();
        float roll = random.nextInt(100) + random.nextFloat();
        if (roll >= 100.0F * com.protoano.granules.world.LuckAdjustedChance.adjusted(com.protoano.granules.config.BalanceConfig.Setting.DISC_CHANCE.floatValue(), luck)) {
            return ItemStack.EMPTY;
        }
        return randomLootDisc(level, random);
    }

    private static ItemStack randomLootDisc(ServerLevel level, RandomSource random) {
        if (ContentManifest.get().isBanned(ContentManifest.Category.UNMARKED_DISCS)) {
            return ItemStack.EMPTY;
        }
        var songs = availableSongs(level.registryAccess());
        if (songs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return recorded(songs.get(random.nextInt(songs.size())), random);
    }

    private static List<String> loadTracks() {
        try (var stream = DiscContent.class.getResourceAsStream("/data/granules/disc_tracks.txt")) {
            if (stream == null) {
                throw new IllegalStateException("Missing unmarked disc track catalog");
            }
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().filter(line -> !line.isBlank()).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read unmarked disc tracks", exception);
        }
    }

    private static Palette loadPalette() {
        try (var stream = DiscContent.class.getResourceAsStream("/data/granules/disc_palette.bin")) {
            if (stream == null) {
                throw new IllegalStateException("Missing unmarked disc palette");
            }
            DataInputStream input = new DataInputStream(stream);
            int width = input.readInt();
            int height = input.readInt();
            int[] values = new int[width * height];
            for (int index = 0; index < values.length; index++) {
                values[index] = input.readInt();
            }
            return new Palette(width, height, values);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read unmarked disc palette", exception);
        }
    }

    private record Palette(int width, int height, int[] values) {
        int at(int x, int y) {
            return values[y * width + x];
        }
    }

    private record BlankColors(long seed, CustomModelData colors) {
    }
}
