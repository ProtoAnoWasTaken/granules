package com.puppy.granules.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.puppy.granules.disc.DiscContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(net.minecraft.server.commands.LootCommand.class)
public abstract class LootCommandUnmarkedDiscMixin {
    @ModifyVariable(method = "drop", at = @At(value = "STORE"), ordinal = 0)
    private static List<ItemStack> granules$rollUnmarkedDisc(
        List<ItemStack> generated,
        CommandContext<CommandSourceStack> context,
        Holder<LootTable> lootTable,
        LootParams lootParams
    ) {
        boolean chestTable = lootTable.unwrapKey()
            .map(key -> key.identifier().getPath().startsWith("chests/"))
            .orElse(false);
        if (!chestTable) {
            return generated;
        }
        ServerLevel level = context.getSource().getLevel();
        ItemStack disc = DiscContent.rollCommandChestLoot(level);
        if (disc.isEmpty()) {
            return generated;
        }
        List<ItemStack> augmented = new ArrayList<>(generated);
        augmented.add(disc);
        return augmented;
    }
}
