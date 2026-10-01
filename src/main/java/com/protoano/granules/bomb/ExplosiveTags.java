package com.protoano.granules.bomb;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ExplosiveTags {
    public static final TagKey<Block> BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("granules", "explosives"));
    public static final TagKey<Item> ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("granules", "explosives"));

    private ExplosiveTags() {
    }
}
