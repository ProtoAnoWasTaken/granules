package com.puppy.granules.client;

import com.puppy.granules.compat.DiscBurnerJadePlugin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public class DiscBurnerJadeProvider implements IBlockComponentProvider {
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData().getCompoundOrEmpty(DiscBurnerJadePlugin.DATA_KEY);
        if (!data.contains("Source") || !data.contains("Target")) {
            return;
        }
        ItemStack source = accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, data.get("Source")).orElse(ItemStack.EMPTY);
        ItemStack target = accessor.decodeFromNbt(ItemStack.OPTIONAL_STREAM_CODEC, data.get("Target")).orElse(ItemStack.EMPTY);
        addDisc(tooltip, "jade.granules.disc_burner.source", source);
        addDisc(tooltip, "jade.granules.disc_burner.target", target);
        int duration = data.getIntOr("Duration", 0);
        int elapsed = data.getIntOr("Progress", 0);
        if (data.getBooleanOr("Burning", false) && duration > 0) {
            float fraction = Math.clamp((float)elapsed / duration, 0.0F, 1.0F);
            ProgressView progress = ProgressView.read(new ProgressView.Data(fraction));
            progress.text = Component.translatable("jade.granules.disc_burner.progress", (int)(fraction * 100), time(elapsed), time(duration));
            tooltip.add(JadeUI.progress(progress));
        } else if (!target.isEmpty()) {
            tooltip.add(Component.translatable("jade.granules.disc_burner.complete"));
        } else {
            tooltip.add(Component.translatable(source.isEmpty()
                ? "jade.granules.disc_burner.insert_source"
                : "jade.granules.disc_burner.insert_blank"));
        }
    }

    private static void addDisc(ITooltip tooltip, String label, ItemStack stack) {
        tooltip.add(Component.translatable(label));
        if (stack.isEmpty()) {
            tooltip.append(Component.translatable("jade.granules.disc_burner.empty"));
            return;
        }
        tooltip.append(JadeUI.smallItem(stack));
        tooltip.append(stack.getHoverName());
        JukeboxSong.fromStack(stack).ifPresent(song -> tooltip.add(song.value().description()));
    }

    private static String time(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public Identifier getUid() {
        return DiscBurnerJadePlugin.UID;
    }
}
