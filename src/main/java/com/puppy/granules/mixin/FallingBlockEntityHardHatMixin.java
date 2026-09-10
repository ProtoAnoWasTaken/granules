package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HardHatProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityHardHatMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void granules$breakOnHardHat(CallbackInfo callbackInfo) {
		FallingBlockEntity fallingBlock = (FallingBlockEntity) (Object) this;
		if (!(fallingBlock.level() instanceof ServerLevel level) || fallingBlock.getDeltaMovement().y >= 0.0D) {
			return;
		}
		BlockState state = fallingBlock.getBlockState();
		if (state.is(BlockTags.ANVIL) || state.is(GranulesMod.ENCHANTED_ANVIL)) {
			return;
		}
		for (Player player : level.players()) {
			if (!player.isAlive() || player.isSpectator() || !HardHatProtection.protects(player, fallingBlock.getBoundingBox())) {
				continue;
			}
			HardHatProtection.absorbImpact(level, player);
			Block.dropResources(state, level, fallingBlock.blockPosition());
			fallingBlock.discard();
			callbackInfo.cancel();
			return;
		}
	}
}
