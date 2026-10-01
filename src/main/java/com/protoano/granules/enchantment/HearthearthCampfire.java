package com.protoano.granules.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class HearthearthCampfire {
    public static final String DATA_KEY = "granules:hearthearth";
    public static final int CAPACITY = 5;
    private final List<PlantedSword> swords = new ArrayList<>();

    public record PlantedSword(UUID owner, ItemStack sword, int style, int progress) {
        public static final Codec<PlantedSword> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(PlantedSword::owner),
            ItemStack.CODEC.fieldOf("sword").forGetter(PlantedSword::sword),
            Codec.intRange(0, CAPACITY - 1).fieldOf("style").forGetter(PlantedSword::style),
            Codec.intRange(0, 36000).optionalFieldOf("progress", 0).forGetter(PlantedSword::progress)
        ).apply(instance, PlantedSword::new));
    }

    public static HearthearthCampfire get(CampfireBlockEntity campfire) {
        return ((HearthearthCampfireAccess) campfire).granules$hearthearth();
    }

    public List<PlantedSword> swords() {
        return List.copyOf(swords);
    }

    public boolean ownsSword(UUID owner) {
        return swords.stream().anyMatch(entry -> entry.owner().equals(owner));
    }

    public boolean plant(CampfireBlockEntity campfire, Player player, ItemStack stack) {
        if (swords.size() >= CAPACITY || ownsSword(player.getUUID()) || !Hearthearth.isSword(stack)) {
            return false;
        }
        if (!(campfire.getLevel() instanceof ServerLevel level)
                || HearthearthOwnership.get(level).hasSword(player)) {
            return false;
        }
        int style = 0;
        for (; style < CAPACITY; style++) {
            int candidate = style;
            if (swords.stream().noneMatch(entry -> entry.style() == candidate)) {
                break;
            }
        }
        swords.add(new PlantedSword(player.getUUID(), stack.consumeAndReturn(1, player), style, 0));
        HearthearthOwnership.get(level).record(campfire, player.getUUID());
        changed(campfire);
        return true;
    }

    public boolean retrieve(CampfireBlockEntity campfire, Player player) {
        for (int index = 0; index < swords.size(); index++) {
            if (swords.get(index).owner().equals(player.getUUID())) {
                ItemStack sword = swords.remove(index).sword();
                if (campfire.getLevel() instanceof ServerLevel level) {
                    HearthearthOwnership.get(level).release(campfire, player.getUUID());
                }
                player.getInventory().add(sword);
                if (!sword.isEmpty()) {
                    player.drop(sword, false);
                }
                changed(campfire);
                return true;
            }
        }
        return false;
    }

    public void tick(CampfireBlockEntity campfire) {
        if (!(campfire.getLevel() instanceof ServerLevel) || !CampfireBlock.isLitCampfire(campfire.getBlockState())) {
            return;
        }
        int interval = campfire.getBlockState().is(Blocks.SOUL_CAMPFIRE)
            ? com.protoano.granules.config.BalanceConfig.Setting.HEARTHEARTH_SOUL_REPAIR_TICKS.intValue()
            : com.protoano.granules.config.BalanceConfig.Setting.HEARTHEARTH_REPAIR_TICKS.intValue();
        boolean repaired = false;
        for (int index = 0; index < swords.size(); index++) {
            PlantedSword entry = swords.get(index);
            if (!entry.sword().isDamaged()) {
                continue;
            }
            int progress = entry.progress() + 1;
            if (progress >= interval) {
                entry.sword().setDamageValue(entry.sword().getDamageValue() - 1);
                progress = 0;
                repaired = true;
            }
            swords.set(index, new PlantedSword(entry.owner(), entry.sword(), entry.style(), progress));
            campfire.setChanged();
        }
        if (repaired) {
            changed(campfire);
        }
    }

    public void dropAll(CampfireBlockEntity campfire) {
        if (campfire.getLevel() instanceof ServerLevel level) {
            for (PlantedSword entry : swords) {
                HearthearthOwnership.get(level).release(campfire, entry.owner());
                Block.popResource(level, campfire.getBlockPos(), entry.sword());
            }
        }
        swords.clear();
    }

    public void load(ValueInput input) {
        swords.clear();
        for (PlantedSword entry : input.read(DATA_KEY, PlantedSword.CODEC.listOf()).orElse(List.of())) {
            if (swords.size() < CAPACITY && !ownsSword(entry.owner()) && !entry.sword().isEmpty()
                    && swords.stream().noneMatch(existing -> existing.style() == entry.style())) {
                swords.add(entry);
            }
        }
    }

    public void save(ValueOutput output) {
        output.store(DATA_KEY, PlantedSword.CODEC.listOf(), swords);
    }

    private static void changed(CampfireBlockEntity campfire) {
        campfire.setChanged();
        if (campfire.getLevel() instanceof ServerLevel level) {
            var state = campfire.getBlockState();
            level.sendBlockUpdated(campfire.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }
}
