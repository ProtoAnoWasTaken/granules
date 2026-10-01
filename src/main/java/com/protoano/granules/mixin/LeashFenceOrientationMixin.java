package com.protoano.granules.mixin;

import com.protoano.granules.block.FenceLeashOrientation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceOrientationMixin extends BlockAttachedEntity {
    protected LeashFenceOrientationMixin(EntityType<? extends BlockAttachedEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "recalculateBoundingBox", at = @At("TAIL"))
    private void granules$rotateHitbox(CallbackInfo callback) {
        var knot = (LeashFenceKnotEntity) (Object) this;
        AABB box = getBoundingBox();
        Vec3 first = FenceLeashOrientation.rotatePosition(knot, new Vec3(box.minX, box.minY, box.minZ));
        Vec3 second = FenceLeashOrientation.rotatePosition(knot, new Vec3(box.maxX, box.maxY, box.maxZ));
        setBoundingBox(new AABB(first, second));
    }

    @Inject(method = "getRopeHoldPosition", at = @At("RETURN"), cancellable = true)
    private void granules$rotateRope(float partialTick, CallbackInfoReturnable<Vec3> callback) {
        callback.setReturnValue(FenceLeashOrientation.rotatePosition((LeashFenceKnotEntity) (Object) this, callback.getReturnValue()));
    }

    @Override
    public void tick() {
        super.tick();
        recalculateBoundingBox();
    }
}
