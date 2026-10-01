package com.protoano.granules.minecart;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class MinecartTrains {
    private static final double MINIMUM_CART_SPACING = 1.9D;
    private static final double TARGET_CART_SPACING = 2.5D;

    private MinecartTrains() {
    }

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (entity instanceof AbstractMinecart cart) {
                return interact(player, hand, cart);
            }
            return InteractionResult.PASS;
        });
    }

    public static InteractionResult interact(Player player, InteractionHand hand, AbstractMinecart cart) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        Leashable leash = (Leashable) cart;
        boolean holdingLead = player.getItemInHand(hand).is(Items.LEAD);
        if (player.getItemInHand(hand).is(Items.SHEARS)) {
            if (((MinecartCoupling) cart).granules$getCoupledCart() != null
                || leash.getLeashHolder() instanceof AbstractMinecart) {
                return InteractionResult.SUCCESS;
            }
            for (AbstractMinecart linked : cart.level().getEntitiesOfClass(
                AbstractMinecart.class, cart.getBoundingBox().inflate(16.0D)
            )) {
                if (((Leashable) linked).getLeashHolder() == cart) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        if (leash.getLeashHolder() == player && holdingLead) {
            if (!cart.level().isClientSide()) {
                leash.dropLeash();
            }
            return InteractionResult.SUCCESS;
        }
        for (AbstractMinecart candidate : cart.level().getEntitiesOfClass(
            AbstractMinecart.class, player.getBoundingBox().inflate(16.0D)
        )) {
            if (candidate != cart && ((Leashable) candidate).getLeashHolder() == player) {
                if (((MinecartCoupling) cart).granules$getCoupledCart() != null
                    && ((MinecartCoupling) cart).granules$getCoupledCart().equals(candidate.getUUID())) {
                    return InteractionResult.SUCCESS;
                }
                if (!cart.level().isClientSide()) {
                    ((MinecartCoupling) candidate).granules$coupleTo(cart);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (holdingLead) {
            if (!leash.isLeashed() && ((MinecartCoupling) cart).granules$getCoupledCart() == null) {
                if (!cart.level().isClientSide()) {
                    leash.setLeashedTo(player, true);
                    player.getItemInHand(hand).consume(1, player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static void pull(AbstractMinecart first, AbstractMinecart second) {
        Vec3 separation = second.position().subtract(first.position());
        double distance = separation.length();
        if (distance < 0.001D) {
            return;
        }
        Vec3 direction = separation.scale(1.0D / distance);
        double firstSpeed = railSpeed(first, direction);
        double secondSpeed = railSpeed(second, direction);
        if (secondSpeed > 0.001D && secondSpeed >= -firstSpeed) {
            follow(first, direction, secondSpeed, distance);
        } else if (firstSpeed < -0.001D) {
            follow(second, direction.scale(-1.0D), -firstSpeed, distance);
        } else if (distance < TARGET_CART_SPACING) {
            double closingSpeed = Math.max(0.0D, firstSpeed - secondSpeed);
            Vec3 braking = direction.scale(closingSpeed * 0.5D);
            first.setDeltaMovement(first.getDeltaMovement().subtract(braking));
            second.setDeltaMovement(second.getDeltaMovement().add(braking));
        }
    }

    public static boolean areCoupled(AbstractMinecart first, AbstractMinecart second) {
        return second.getUUID().equals(((MinecartCoupling) first).granules$getCoupledCart())
            || first.getUUID().equals(((MinecartCoupling) second).granules$getCoupledCart())
            || ((Leashable) first).getLeashHolder() == second
            || ((Leashable) second).getLeashHolder() == first;
    }

    private static double railSpeed(AbstractMinecart cart, Vec3 direction) {
        double limit = ((MinecartCoupling) cart).granules$getRailSpeedLimit();
        return Math.clamp(cart.getDeltaMovement().dot(direction) * movementFactor(cart), -limit, limit);
    }

    private static double movementFactor(AbstractMinecart cart) {
        if (cart.isVehicle() && !AbstractMinecart.useExperimentalMovement(cart.level())) {
            return 0.75D;
        }
        return 1.0D;
    }

    private static void follow(AbstractMinecart follower, Vec3 direction, double leaderSpeed, double distance) {
        double spacingCorrection = Math.clamp((distance - TARGET_CART_SPACING) * 0.2D, -leaderSpeed, 0.08D);
        double availableTravel = Math.max(0.0D, distance - MINIMUM_CART_SPACING);
        double desiredSpeed = Math.min(leaderSpeed + spacingCorrection, availableTravel);
        ((MinecartCoupling) follower).granules$allowTowingSpeed(desiredSpeed);
        double factor = movementFactor(follower);
        double currentSpeed = follower.getDeltaMovement().dot(direction) * factor;
        double nextSpeed = Math.min(currentSpeed + Math.clamp(desiredSpeed - currentSpeed, -0.12D, 0.08D), availableTravel);
        double acceleration = (nextSpeed - currentSpeed) / factor;
        follower.setDeltaMovement(follower.getDeltaMovement().add(direction.scale(acceleration)));
        follower.needsSync = true;
    }
}
