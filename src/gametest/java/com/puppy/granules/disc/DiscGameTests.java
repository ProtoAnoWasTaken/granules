package com.puppy.granules.disc;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class DiscGameTests {
    @GameTest
    public void blankDiscIsTaggedAndEjectedFromJukebox(GameTestHelper helper) {
        TagKey<net.minecraft.world.item.Item> commonMusicDiscs = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "music_discs")
        );
        TagKey<net.minecraft.world.item.Item> minecraftMusicDiscs = TagKey.create(
            Registries.ITEM,
            Identifier.withDefaultNamespace("music_discs")
        );
        require(new ItemStack(DiscContent.DISC).is(commonMusicDiscs), "Unmarked Discs must use the common music disc tag");
        require(new ItemStack(DiscContent.DISC).is(minecraftMusicDiscs), "Unmarked Discs must expose the Minecraft music disc tag");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.JUKEBOX);
        JukeboxBlockEntity jukebox = helper.getBlockEntity(pos, JukeboxBlockEntity.class);
        jukebox.setTheItem(new ItemStack(DiscContent.DISC));
        require(jukebox.getTheItem().isEmpty(), "A Jukebox must not retain a blank Unmarked Disc");
        var drops = helper.getEntities(EntityTypes.ITEM, pos.above(), 2.0);
        require(drops.stream().anyMatch(entity -> DiscContent.isBlank(entity.getItem())), "A rejected blank disc must pop out above the Jukebox");
        helper.succeed();
    }

    @GameTest
    public void jadeTransmitsBothInputsAndLiveProgress(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, DiscContent.BURNER);
        DiscBurnerBlockEntity burner = helper.getBlockEntity(pos, DiscBurnerBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        burner.insert(new ItemStack(Items.MUSIC_DISC_CAT), player);
        burner.insert(new ItemStack(DiscContent.DISC), player);
        for (int tick = 0; tick < 80; tick++) {
            DiscBurnerBlockEntity.tick(helper.getLevel(), burner.getBlockPos(), burner.getBlockState(), burner);
        }
        var data = new net.minecraft.nbt.CompoundTag();
        var accessor = new snownee.jade.impl.BlockAccessorImpl.Builder()
            .level(helper.getLevel())
            .player(player)
            .serverData(data)
            .serverConnected(true)
            .blockState(burner.getBlockState())
            .blockEntity(() -> burner)
            .hit(new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(burner.getBlockPos()),
                net.minecraft.core.Direction.UP,
                burner.getBlockPos(),
                false
            ))
            .build();
        var providers = snownee.jade.impl.WailaCommonRegistration.instance()
            .blockDataProvidersOf(burner.getBlockState(), burner, true);
        var provider = providers.stream()
            .filter(candidate -> candidate.getUid().equals(com.puppy.granules.compat.DiscBurnerJadePlugin.UID))
            .findFirst().orElseThrow();
        provider.appendServerData(data, accessor);
        var status = data.getCompoundOrEmpty(com.puppy.granules.compat.DiscBurnerJadePlugin.DATA_KEY);
        ItemStack source = accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, status.get("Source")).orElseThrow();
        ItemStack target = accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, status.get("Target")).orElseThrow();
        require(source.is(Items.MUSIC_DISC_CAT), "Jade must receive the actual source item");
        require(DiscContent.isBlank(target), "Jade must receive the second input");
        require(status.getIntOr("Progress", -1) == 80, "Jade must receive current copying progress");
        require(status.getIntOr("Duration", -1) == burner.getCopyDuration(), "Jade must receive the track duration");
        require(status.getBooleanOr("Burning", false), "Jade must receive the copying state");
        helper.succeed();
    }

    @GameTest
    public void copyingRequiresFullTrackAndPreservesSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, DiscContent.BURNER);
        DiscBurnerBlockEntity burner = helper.getBlockEntity(pos, DiscBurnerBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack source = new ItemStack(Items.MUSIC_DISC_13);
        ItemStack original = source.copy();
        burner.insert(new ItemStack(DiscContent.DISC), player);
        require(!burner.isBurning(), "A blank disc must not be accepted as the source");
        burner.insert(source, player);
        require(source.isEmpty(), "Source insertion must transfer one disc");
        require(!burner.isBurning(), "Copying must wait for the blank disc");
        ItemStack blank = new ItemStack(DiscContent.DISC);
        burner.insert(blank, player);
        require(blank.isEmpty() && burner.isBurning(), "Copying must start with both discs");
        burner.extract(player);
        require(player.getInventory().isEmpty(), "Extraction must be blocked during copying");
        var saved = burner.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = BlockEntity.loadStatic(burner.getBlockPos(), burner.getBlockState(), saved, helper.getLevel().registryAccess());
        require(restored instanceof DiscBurnerBlockEntity, "Saved burner must load with the correct type");
        burner = (DiscBurnerBlockEntity)restored;
        helper.getLevel().setBlockEntity(burner);
        require(burner.isBurning(), "Both inputs and the copying state must survive reloading");
        int duration = JukeboxSong.fromStack(original).orElseThrow().value().lengthInTicks();
        require(burner.getCopyDuration() == duration, "Jade duration must match the song length");
        require(ItemStack.isSameItemSameComponents(original, burner.getSourceDisc()), "Jade must expose the source disc");
        require(DiscContent.isBlank(burner.getTargetDisc()), "Jade must expose the unrecorded target while copying");
        for (int tick = 0; tick < duration; tick++) {
            DiscBurnerBlockEntity.tick(helper.getLevel(), burner.getBlockPos(), burner.getBlockState(), burner);
            if (tick == duration / 2) {
                require(burner.getCopyProgress() == tick + 1, "Jade progress must advance with copying");
            }
        }
        require(burner.isBurning(), "Copying must not finish early");
        DiscBurnerBlockEntity.tick(helper.getLevel(), burner.getBlockPos(), burner.getBlockState(), burner);
        require(!burner.isBurning(), "Copying must finish at the song boundary");
        require(burner.getCopyProgress() == duration, "Completed copying must report full progress");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.QUARTZ));
        helper.useBlock(pos, player);
        require(player.getInventory().countItem(DiscContent.DISC) == 0, "A nonempty hand must not remove the discs");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(pos, player);
        ItemStack foundOriginal = ItemStack.EMPTY;
        ItemStack foundCopy = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.MUSIC_DISC_13)) {
                foundOriginal = stack;
            }
            if (stack.is(DiscContent.DISC)) {
                foundCopy = stack;
            }
        }
        require(ItemStack.isSameItemSameComponents(original, foundOriginal), "Original disc must remain unchanged");
        require(foundCopy.has(DataComponents.CUSTOM_MODEL_DATA), "Burned disc needs a stored palette");
        require(foundCopy.getRarity() == Rarity.EPIC, "Burned discs must have Epic rarity");
        require(burner.getCopyProgress() == 0 && burner.getSourceDisc().isEmpty() && burner.getTargetDisc().isEmpty(), "Jade must clear after extraction");
        require(original.get(DataComponents.JUKEBOX_PLAYABLE).equals(foundCopy.get(DataComponents.JUKEBOX_PLAYABLE)), "Copied song must match the source");
        helper.succeed();
    }

    @GameTest
    public void breakingBurnerReturnsBothInputsWithoutFinishingCopy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, DiscContent.BURNER);
        DiscBurnerBlockEntity burner = helper.getBlockEntity(pos, DiscBurnerBlockEntity.class);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        burner.insert(new ItemStack(Items.MUSIC_DISC_CAT), player);
        burner.insert(new ItemStack(DiscContent.DISC), player);
        require(burner.isBurning(), "Burner must be copying before it is broken");
        helper.setBlock(pos, Blocks.AIR);
        var drops = helper.getEntities(EntityTypes.ITEM, pos, 2.0);
        int originals = 0;
        int blanks = 0;
        for (var entity : drops) {
            ItemStack stack = entity.getItem();
            if (stack.is(Items.MUSIC_DISC_CAT)) {
                originals += stack.getCount();
            }
            if (DiscContent.isBlank(stack)) {
                blanks += stack.getCount();
            }
        }
        require(originals == 1 && blanks == 1, "Breaking must return one original and one unrecorded disc");
        helper.succeed();
    }

    @GameTest
    public void blankPalettesAndRecordedTracks(GameTestHelper helper) {
        ItemStack first = new ItemStack(DiscContent.DISC);
        ItemStack second = new ItemStack(DiscContent.DISC);
        DiscContent.colorBlank(first, helper.getLevel());
        DiscContent.colorBlank(second, helper.getLevel());
        require(first.get(DataComponents.CUSTOM_MODEL_DATA).equals(second.get(DataComponents.CUSTOM_MODEL_DATA)), "Blank palettes must agree within a world");
        require(first.get(DataComponents.CUSTOM_MODEL_DATA).colors().getFirst() == 0xFE3330, "Blank base must use the center of the supplied map");
        var songs = helper.getLevel().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).listElements()
            .filter(song -> song.key().identifier().getNamespace().equals("granules"))
            .filter(song -> song.key().identifier().getPath().startsWith("unmarked_disc/"))
            .toList();
        require(songs.size() == 75, "All 75 background tracks must load");
        for (String track : List.of("eat_your_potatoes", "world_of_synthesis", "for_the_sake_of_making_games", "voyager", "lotus")) {
            Identifier music = Identifier.fromNamespaceAndPath("granules", "music." + track);
            Identifier disc = Identifier.fromNamespaceAndPath("granules", "unmarked_disc.burrow_" + track);
            require(BuiltInRegistries.SOUND_EVENT.containsKey(music), "Burrow music event must be registered: " + music);
            require(BuiltInRegistries.SOUND_EVENT.containsKey(disc), "Burrow disc event must be registered: " + disc);
        }
        for (var song : songs) {
            ItemStack disc = DiscContent.recorded(song, RandomSource.create(42));
            var palette = disc.get(DataComponents.CUSTOM_MODEL_DATA);
            DiscContent.colorBlank(disc, helper.getLevel());
            require(palette.equals(disc.get(DataComponents.CUSTOM_MODEL_DATA)), "Recorded palettes must remain stable");
            require(JukeboxSong.fromStack(disc).orElseThrow().equals(song), "Recorded discs must be playable");
            require(song.value().lengthInSeconds() > 0, "Track duration must be positive");
        }
        helper.succeed();
    }

    @GameTest
    public void chestLootNeverOverwritesAndRollsOnce(GameTestHelper helper) {
        SimpleContainer chest = new SimpleContainer(27);
        ItemStack marker = new ItemStack(Items.DIAMOND, 64);
        for (int slot = 0; slot < 27; slot++) {
            chest.setItem(slot, marker.copy());
        }
        for (long seed = 1; seed <= 2000; seed++) {
            DiscContent.addChestLoot(chest, helper.getLevel(), seed, 1234);
        }
        for (int slot = 0; slot < 27; slot++) {
            require(ItemStack.matches(marker, chest.getItem(slot)), "Full chest loot must remain unchanged");
        }
        int generated = 0;
        for (long seed = 1; seed <= 10000; seed++) {
            chest.setItem(13, ItemStack.EMPTY);
            DiscContent.addChestLoot(chest, helper.getLevel(), seed, 1234);
            if (!chest.getItem(13).isEmpty()) {
                require(chest.getItem(13).is(DiscContent.DISC), "Only the free slot may receive a disc");
                require(chest.getItem(13).has(DataComponents.JUKEBOX_PLAYABLE), "Loot discs must contain music");
                generated++;
            }
        }
        require(generated >= 50 && generated <= 150, "Disc frequency must be consistent with one percent");
        int commandGenerated = 0;
        for (int roll = 0; roll < 10000; roll++) {
            ItemStack commandDisc = DiscContent.rollCommandChestLoot(helper.getLevel());
            if (!commandDisc.isEmpty()) {
                require(commandDisc.is(DiscContent.DISC), "Command chest rolls must produce Unmarked Discs");
                require(commandDisc.has(DataComponents.JUKEBOX_PLAYABLE), "Command chest rolls must produce recorded discs");
                commandGenerated++;
            }
        }
        require(commandGenerated >= 50 && commandGenerated <= 150, "Command chest loot frequency must be consistent with one percent");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity blockChest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        blockChest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon")), 42);
        blockChest.unpackLootTable(null);
        require(blockChest.getLootTable() == null, "Generated chest loot must unpack only once");
        ItemStack[] snapshot = new ItemStack[27];
        for (int slot = 0; slot < 27; slot++) {
            snapshot[slot] = blockChest.getItem(slot).copy();
        }
        blockChest.unpackLootTable(null);
        for (int slot = 0; slot < 27; slot++) {
            require(ItemStack.matches(snapshot[slot], blockChest.getItem(slot)), "Reopening a chest must not reroll loot");
        }
        helper.succeed();
    }

    @GameTest
    public void dungeonProduceUsesInclusiveRequestedRanges(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
        boolean[] sawEmpty = new boolean[3];
        boolean[] sawPresent = new boolean[3];
        for (long seed = 1; seed <= 128; seed++) {
            chest.clearContent();
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("chests/simple_dungeon")), seed);
            chest.unpackLootTable(null);
            int carrots = count(chest, Items.CARROT);
            int potatoes = count(chest, Items.POTATO);
            int mushrooms = count(chest, Items.RED_MUSHROOM);
            require(carrots >= 0 && carrots <= 2, "Dungeon carrot loot must remain within zero to two");
            require(potatoes >= 0 && potatoes <= 1, "Dungeon potato loot must remain within zero to one");
            require(mushrooms >= 0 && mushrooms <= 3, "Dungeon red mushroom loot must remain within zero to three");
            sawEmpty[0] |= carrots == 0;
            sawEmpty[1] |= potatoes == 0;
            sawEmpty[2] |= mushrooms == 0;
            sawPresent[0] |= carrots > 0;
            sawPresent[1] |= potatoes > 0;
            sawPresent[2] |= mushrooms > 0;
        }
        for (int index = 0; index < 3; index++) {
            require(sawEmpty[index], "Each dungeon produce item must sometimes be absent");
            require(sawPresent[index], "Each dungeon produce item must sometimes be present");
        }
        helper.succeed();
    }

    private static int count(ChestBlockEntity chest, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (chest.getItem(slot).is(item)) {
                count += chest.getItem(slot).getCount();
            }
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
