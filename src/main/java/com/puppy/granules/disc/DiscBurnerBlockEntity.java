package com.puppy.granules.disc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DiscBurnerBlockEntity extends BlockEntity {
    private ItemStack source = ItemStack.EMPTY;
    private ItemStack target = ItemStack.EMPTY;
    private boolean burning;
    private boolean resumePlayback;
    private final JukeboxSongPlayer playback = new JukeboxSongPlayer(this::changed, getBlockPos());

    public DiscBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(DiscContent.BURNER_ENTITY, pos, state);
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public boolean isBurning() {
        return burning;
    }

    public ItemStack getSourceDisc() {
        return source.copy();
    }

    public ItemStack getTargetDisc() {
        return target.copy();
    }

    public int getCopyDuration() {
        return JukeboxSong.fromStack(source).map(song -> song.value().lengthInTicks()).orElse(0);
    }

    public int getCopyProgress() {
        if (burning) {
            return (int)Math.min(playback.getTicksSinceSongStarted(), getCopyDuration());
        }
        if (target.has(DataComponents.JUKEBOX_PLAYABLE)) {
            return getCopyDuration();
        }
        return 0;
    }

    public int comparatorOutput() {
        return JukeboxSong.fromStack(source).map(song -> song.value().comparatorOutput()).orElse(0);
    }

    public void insert(ItemStack stack, Player player) {
        if (burning || stack.isEmpty()) {
            return;
        }
        if (source.isEmpty() && stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            source = stack.consumeAndReturn(1, player);
            changed();
        } else if (!source.isEmpty() && target.isEmpty() && DiscContent.isBlank(stack)) {
            target = stack.consumeAndReturn(1, player);
            JukeboxSong.fromStack(source).ifPresent(song -> {
                burning = true;
                playback.play(level, song);
            });
            changed();
        }
    }

    public void extract(Player player) {
        if (burning) {
            return;
        }
        ItemStack original = source;
        ItemStack copy = target;
        source = ItemStack.EMPTY;
        target = ItemStack.EMPTY;
        changed();
        if (!original.isEmpty() && !player.getInventory().add(original)) {
            player.drop(original, false);
        }
        if (!copy.isEmpty() && !player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DiscBurnerBlockEntity burner) {
        if (!burner.burning || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (burner.resumePlayback) {
            burner.resumePlayback = false;
            JukeboxSong.fromStack(burner.source).ifPresent(song -> burner.playback.play(level, song));
        }
        JukeboxSong song = burner.playback.getSong();
        if (song == null || !DiscContent.isBlank(burner.target)) {
            burner.burning = false;
            burner.playback.stop(level, state);
            burner.changed();
            return;
        }
        if (burner.playback.getTicksSinceSongStarted() >= song.lengthInTicks()) {
            burner.target.set(DataComponents.JUKEBOX_PLAYABLE, burner.source.get(DataComponents.JUKEBOX_PLAYABLE));
            burner.target.set(DataComponents.RARITY, Rarity.EPIC);
            DiscContent.colorRecorded(burner.target, serverLevel.getRandom());
            burner.burning = false;
            burner.playback.stop(level, state);
            burner.changed();
            return;
        }
        burner.playback.tick(level, state);
        burner.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Source", ItemStack.OPTIONAL_CODEC, source);
        output.store("Target", ItemStack.OPTIONAL_CODEC, target);
        output.putBoolean("Burning", burning);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        source = input.read("Source", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        target = input.read("Target", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        burning = input.getBooleanOr("Burning", false) && source.has(DataComponents.JUKEBOX_PLAYABLE) && DiscContent.isBlank(target);
        resumePlayback = burning;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        playback.stop(level, state);
        Block.popResource(level, pos, source);
        Block.popResource(level, pos, target);
        source = ItemStack.EMPTY;
        target = ItemStack.EMPTY;
        burning = false;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            playback.stop(level, getBlockState());
        }
        super.setRemoved();
    }
}
