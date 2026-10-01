package com.protoano.granules.mixin;

import com.protoano.granules.item.StateLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkCatalystBlockEntity.CatalystListener.class)
public abstract class StateLockCatalystListenerMixin {
    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void granules$ignoreDeaths(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 eventPos, CallbackInfoReturnable<Boolean> callback) {
        var listener = (SculkCatalystBlockEntity.CatalystListener) (Object) this;
        var source = listener.getListenerSource().getPosition(level);
        if (source.isPresent() && StateLocks.locked(level.getBlockState(BlockPos.containing(source.get())))) {
            callback.setReturnValue(false);
        }
    }
}
