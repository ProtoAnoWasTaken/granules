package com.puppy.granules.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.puppy.granules.fletching.FletchingMenu;
import com.puppy.granules.GranulesMod;
import com.puppy.granules.config.ContentManifest;

@Mixin(BlockBehaviour.class)
public class CraftingTableBlockMixin {
	@Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
	private void openFletchingMenu(
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		BlockHitResult hitResult,
		CallbackInfoReturnable<InteractionResult> callback
	) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
			return;
		}
		if (!state.is(Blocks.FLETCHING_TABLE) && !state.is(GranulesMod.ENCHANTED_FLETCHING_TABLE)) {
			return;
		}

		if (!level.isClientSide()) {
			player.openMenu(this.createFletchingMenu(level, pos));
		}

		callback.setReturnValue(InteractionResult.SUCCESS);
	}

	@Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
	private void provideFletchingMenu(
		BlockState state,
		Level level,
		BlockPos pos,
		CallbackInfoReturnable<MenuProvider> callback
	) {
		if (ContentManifest.get().isBanned(ContentManifest.Category.ENHANCED_WORKSTATIONS)) {
			return;
		}
		if (state.is(Blocks.FLETCHING_TABLE) || state.is(GranulesMod.ENCHANTED_FLETCHING_TABLE)) {
			callback.setReturnValue(this.createFletchingMenu(level, pos));
		}
	}

	private MenuProvider createFletchingMenu(Level level, BlockPos pos) {
		return new SimpleMenuProvider(
			(containerId, inventory, player) -> new FletchingMenu(
				containerId,
				inventory,
				ContainerLevelAccess.create(level, pos)
			),
			Component.translatable("container.granules.fletching")
		);
	}
}
