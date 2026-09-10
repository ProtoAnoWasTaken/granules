package com.puppy.granules.mixin;

import com.puppy.granules.access.ServerPlayerGameModeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin implements ServerPlayerGameModeAccess {
	@Shadow
	private boolean isDestroyingBlock;

	@Shadow
	private BlockPos destroyPos;

	@Override
	public boolean granules$isDestroyingBlock() {
		return this.isDestroyingBlock;
	}

	@Override
	public BlockPos granules$getDestroyPos() {
		return this.destroyPos;
	}
}
