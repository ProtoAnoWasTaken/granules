package com.protoano.granules.minecart;

import java.util.UUID;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

public interface MinecartCoupling {
    UUID granules$getCoupledCart();

    void granules$coupleTo(AbstractMinecart cart);

    void granules$tickCoupling();

    double granules$getRailSpeedLimit();

    void granules$allowTowingSpeed(double speed);
}
