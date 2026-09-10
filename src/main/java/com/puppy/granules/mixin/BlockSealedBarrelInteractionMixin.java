package com.puppy.granules.mixin;

import com.puppy.granules.world.SealedBarrelProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockSealedBarrelInteractionMixin {
	@Inject(method = "playerWillDestroy", at = @At("HEAD"))
	private void granules$dropCreativeSealedBarrel(
		Level level,
		BlockPos pos,
		BlockState state,
		Player player,
		CallbackInfoReturnable<BlockState> callbackInfo
	) {
		if (level.isClientSide()
			|| !player.preventsBlockDrops()
			|| !(state.getBlock() instanceof BarrelBlock)
			|| !state.getValue(SealedBarrelProperties.SEALED)) {
			return;
		}
		ItemStack barrel = new ItemStack(Items.BARREL);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity != null) {
			barrel.applyComponents(blockEntity.collectComponents());
		}
		CustomData sealedData = CustomData.EMPTY.update(tag -> tag.putBoolean(SealedBarrelProperties.DATA_KEY, true));
		barrel.set(DataComponents.CUSTOM_DATA, sealedData);
		barrel.set(DataComponents.ITEM_MODEL, SealedBarrelProperties.ITEM_MODEL);
		barrel.set(DataComponents.ITEM_NAME, Component.translatable("block.granules.sealed_barrel"));
		ItemEntity droppedBarrel = new ItemEntity(
			level,
			pos.getX() + 0.5D,
			pos.getY() + 0.5D,
			pos.getZ() + 0.5D,
			barrel
		);
		droppedBarrel.setDefaultPickUpDelay();
		level.addFreshEntity(droppedBarrel);
	}

}
