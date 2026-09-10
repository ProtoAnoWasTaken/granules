package com.puppy.granules.mixin;

import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrafterBlock.class)
public abstract class CrafterAdvancementMixin {
	@Inject(method = "dispenseItem", at = @At("HEAD"))
	private void granules$awardCrafterCraftingCrafter(
		ServerLevel level,
		BlockPos pos,
		CrafterBlockEntity crafter,
		ItemStack result,
		BlockState state,
		RecipeHolder<?> recipe,
		CallbackInfo callbackInfo
	) {
		if (!result.is(Items.CRAFTER)) {
			return;
		}
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(16.0D))) {
			GranulesAdvancements.award(player, "machines_make_machines");
		}
	}
}
