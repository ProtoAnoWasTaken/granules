package com.protoano.granules.client.mixin;

import com.protoano.granules.client.ClientGraveGhost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class GraveGhostVisibilityMixin {
    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void showBuriedGhost(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof ClientGraveGhost ghost) {
            Minecraft client = Minecraft.getInstance();
            callback.setReturnValue(ghost.visibleTo(client.player)
                && client.getEntityRenderDispatcher().shouldRender(ghost, frustum, x, y, z));
        }
    }
}
