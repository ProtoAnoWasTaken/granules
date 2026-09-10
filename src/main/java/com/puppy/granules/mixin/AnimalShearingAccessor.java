package com.puppy.granules.mixin;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface AnimalShearingAccessor {
	@Invoker("dropFromShearingLootTable")
	void granules$dropFromShearingLootTable(
		ServerLevel level,
		ResourceKey<LootTable> lootTable,
		ItemInstance tool,
		BiConsumer<ServerLevel, ItemStack> output
	);
}
