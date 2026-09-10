package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.HoneyCauldrons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {
	private static final int HONEY_DRIP_INTERVAL = 600;

	@Inject(method = "serverTick", at = @At("TAIL"))
	private static void granules$fillCauldronWithHoney(Level level, BlockPos pos, BlockState state, BeehiveBlockEntity beehive, CallbackInfo callbackInfo) {
		if (state.getValue(BeehiveBlock.HONEY_LEVEL) < BeehiveBlock.MAX_HONEY_LEVELS || level.getRandom().nextInt(HONEY_DRIP_INTERVAL) != 0) {
			return;
		}

		for (int y = pos.getY() - 1; y >= level.getMinY(); y--) {
			BlockPos cauldronPos = new BlockPos(pos.getX(), y, pos.getZ());
			BlockState cauldronState = level.getBlockState(cauldronPos);
			BlockState newState;
			if (cauldronState.is(Blocks.CAULDRON) && !HoneyCauldrons.isFull(cauldronState)) {
				newState = HoneyCauldrons.withHoneyLevel(cauldronState, HoneyCauldrons.getHoneyLevel(cauldronState) + 1);
			} else if (cauldronState.isAir()) {
				continue;
			} else {
				return;
			}

			level.setBlockAndUpdate(cauldronPos, newState);
			level.gameEvent(GameEvent.BLOCK_CHANGE, cauldronPos, GameEvent.Context.of(newState));
			return;
		}
	}
}
