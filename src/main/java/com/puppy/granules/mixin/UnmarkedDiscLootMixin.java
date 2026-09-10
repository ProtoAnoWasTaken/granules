package com.puppy.granules.mixin;

import com.puppy.granules.disc.DiscContent;
import com.puppy.granules.config.ContentManifest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RandomizableContainer.class)
public interface UnmarkedDiscLootMixin {
    @Redirect(method = "unpackLootTable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;fill(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/storage/loot/LootParams;J)V"))
    private void granules$addUnmarkedDisc(LootTable table, Container container, LootParams params, long seed) {
        table.fill(container, params, seed);
        if (!ContentManifest.get().isBanned(ContentManifest.Category.UNMARKED_DISCS)
            && container instanceof ChestBlockEntity chest
            && chest.getLevel() instanceof ServerLevel level) {
            DiscContent.addChestLoot(container, level, seed, chest.getBlockPos().asLong());
        }
    }
}
