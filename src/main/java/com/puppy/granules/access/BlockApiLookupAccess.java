package com.puppy.granules.mixin.access;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.custom.ApiProviderMap;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.fabricmc.fabric.impl.lookup.block.BlockApiLookupImpl")
public interface BlockApiLookupAccess {
	@Accessor("providerMap")
	ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> granules$getProviderMap();

	@Mutable
	@Accessor("providerMap")
	void granules$setProviderMap(ApiProviderMap<Block, BlockApiLookup.BlockApiProvider<?, ?>> providerMap);
}
