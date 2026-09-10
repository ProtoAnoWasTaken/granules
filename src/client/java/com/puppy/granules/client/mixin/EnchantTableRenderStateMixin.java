package com.puppy.granules.client.mixin;

import com.puppy.granules.client.EnchantedEnchantTableRenderStateAccess;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EnchantTableRenderState.class)
public class EnchantTableRenderStateMixin implements EnchantedEnchantTableRenderStateAccess {
	@Unique
	private boolean granules$enchantedEnchantingTable;

	@Override
	public boolean granules$isEnchantedEnchantingTable() {
		return granules$enchantedEnchantingTable;
	}

	@Override
	public void granules$setEnchantedEnchantingTable(boolean enchanted) {
		granules$enchantedEnchantingTable = enchanted;
	}
}
