package com.protoano.granules.mixin;

import com.protoano.granules.minecart.MinecartCoupling;
import com.protoano.granules.minecart.MinecartTrains;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartLeashMixin extends Entity implements Leashable, MinecartCoupling {
    @Unique
    private LeashData granules$leashData;

    @Unique
    private UUID granules$coupledCart;

    @Unique
    private double granules$towingSpeed;

    @Unique
    private long granules$towingSpeedExpires = Long.MIN_VALUE;

    @Override
    public void granules$allowTowingSpeed(double speed) {
        granules$towingSpeed = speed;
        granules$towingSpeedExpires = level().getGameTime() + 1L;
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void granules$matchTowingSpeed(ServerLevel level, CallbackInfoReturnable<Double> callback) {
        AbstractMinecart cart = (AbstractMinecart) (Object) this;
        if (!cart.isFurnace() && level.getGameTime() <= granules$towingSpeedExpires) {
            callback.setReturnValue(Math.max(callback.getReturnValue(), granules$towingSpeed));
        }
    }

    @Shadow
    protected abstract double getMaxSpeed(ServerLevel level);

    @Override
    public double granules$getRailSpeedLimit() {
        if (level() instanceof ServerLevel serverLevel) {
            return getMaxSpeed(serverLevel);
        }
        return 0.4D;
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void granules$avoidCoupledCollision(Entity entity, CallbackInfo callback) {
        if (entity instanceof AbstractMinecart other
            && MinecartTrains.areCoupled((AbstractMinecart) (Object) this, other)) {
            callback.cancel();
        }
    }

    protected AbstractMinecartLeashMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public LeashData getLeashData() {
        return granules$leashData;
    }

    @Override
    public void setLeashData(LeashData data) {
        granules$leashData = data;
    }

    @Override
    public UUID granules$getCoupledCart() {
        return granules$coupledCart;
    }

    @Override
    public void granules$coupleTo(AbstractMinecart cart) {
        granules$coupledCart = cart.getUUID();
        setLeashedTo(cart, true);
    }

    @Override
    public void onLeashRemoved() {
        granules$coupledCart = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !level().isClientSide()) {
            dropLeash();
        }
        super.remove(reason);
    }

	@Inject(method = "tick", at = @At("HEAD"))
    private void granules$tickLeash(CallbackInfo callback) {
        if (level() instanceof ServerLevel serverLevel) {
            Leashable.tickLeash(serverLevel, this);
		}
	}

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void granules$readLeash(ValueInput input, CallbackInfo callback) {
        readLeashData(input);
        granules$coupledCart = input.read("granules:coupled_cart", UUIDUtil.CODEC).orElse(null);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void granules$saveLeash(ValueOutput output, CallbackInfo callback) {
        writeLeashData(output, granules$leashData);
        output.storeNullable("granules:coupled_cart", UUIDUtil.CODEC, granules$coupledCart);
    }

    @Override
    public void granules$tickCoupling() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity holder = getLeashHolder();
        if (holder != null && holder.isRemoved()) {
            if (holder.getRemovalReason().shouldDestroy()) {
                dropLeash();
                return;
            }
            setLeashData(null);
        }
        Entity resolved = serverLevel.getEntity(granules$coupledCart);
        if (resolved instanceof AbstractMinecart other && !other.isRemoved()) {
            if (getLeashHolder() != other) {
                setLeashedTo(other, true);
            }
            MinecartTrains.pull((AbstractMinecart) (Object) this, other);
        }
    }
}
