package com.protoano.granules.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.protoano.granules.config.BalanceConfig;
import com.protoano.granules.config.ContentManifest;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CopperGolemUpgrades {
    public record Upgrades(int eyes, boolean priority, boolean bundle) {
        public static final Upgrades NONE = new Upgrades(0, false, false);
        public static final Codec<Upgrades> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 2).fieldOf("eyes").forGetter(Upgrades::eyes),
            Codec.BOOL.fieldOf("priority").forGetter(Upgrades::priority),
            Codec.BOOL.fieldOf("bundle").forGetter(Upgrades::bundle)
        ).apply(instance, Upgrades::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Upgrades> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Upgrades::eyes,
            ByteBufCodecs.BOOL, Upgrades::priority,
            ByteBufCodecs.BOOL, Upgrades::bundle, Upgrades::new);
    }

    public static final AttachmentType<Upgrades> UPGRADES = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("granules", "copper_golem_upgrades"), builder -> builder
            .persistent(Upgrades.CODEC).syncWith(Upgrades.STREAM_CODEC, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> FUEL = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("granules", "copper_golem_redstone"), builder -> builder.persistent(Codec.intRange(0, 32000)));
    private static final Identifier SPEED = Identifier.fromNamespaceAndPath("granules", "redstone_golem_speed");
    private static final Identifier RANGE = Identifier.fromNamespaceAndPath("granules", "ender_golem_range");

    private CopperGolemUpgrades() {
    }

    public static void initialize() {
    }

    public static boolean enabled() {
        return !ContentManifest.get().isBanned(ContentManifest.Category.COPPER_GOLEM_UPGRADES);
    }

    public static Upgrades get(CopperGolem golem) {
        return enabled() ? golem.getAttachedOrElse(UPGRADES, Upgrades.NONE) : Upgrades.NONE;
    }

    public static double rangeMultiplier(CopperGolem golem) {
        return Math.pow(BalanceConfig.Setting.COPPER_GOLEM_EYE_MULTIPLIER.value(), get(golem).eyes());
    }

    public static boolean boosted(CopperGolem golem) {
        return enabled() && golem.getAttachedOrElse(FUEL, 0) > 0;
    }

    public static InteractionResult interact(CopperGolem golem, Player player, InteractionHand hand) {
        if (!enabled() || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Upgrades old = get(golem);
        Upgrades upgraded = old;
        int fuel = golem.getAttachedOrElse(FUEL, 0);
        int fuelPerItem = BalanceConfig.Setting.COPPER_GOLEM_REDSTONE_TICKS.intValue();
        boolean redstone = stack.is(Items.REDSTONE) && fuel <= 32000 - fuelPerItem;
        if (stack.is(Items.ENDER_EYE) && old.eyes() < 2) {
            upgraded = new Upgrades(old.eyes() + 1, old.priority(), old.bundle());
        } else if (stack.is(Items.ITEM_FRAME) && !old.priority()) {
            upgraded = new Upgrades(old.eyes(), true, old.bundle());
        } else if (stack.has(DataComponents.BUNDLE_CONTENTS) && stack.get(DataComponents.BUNDLE_CONTENTS).isEmpty() && !old.bundle()) {
            upgraded = new Upgrades(old.eyes(), old.priority(), true);
        }
        if (upgraded.equals(old) && !redstone) {
            return InteractionResult.PASS;
        }
        if (golem.level() instanceof ServerLevel level) {
            if (redstone) {
                golem.setAttached(FUEL, fuel + fuelPerItem);
            } else {
                golem.setAttached(UPGRADES, upgraded);
                if (upgraded.eyes() == 2 && old.eyes() < 2 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    com.protoano.granules.advancement.GranulesAdvancements.award(serverPlayer, "double_vision");
                }
            }
            stack.consume(1, player);
            golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
            golem.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
            golem.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, golem.getX(), golem.getY() + 0.8, golem.getZ(), 6, 0.25, 0.3, 0.25, 0);
        }
        return InteractionResult.SUCCESS;
    }

    public static void tick(CopperGolem golem) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return;
        }
        boolean active = boosted(golem);
        updateModifier(golem, Attributes.MOVEMENT_SPEED, SPEED, active ? BalanceConfig.Setting.COPPER_GOLEM_REDSTONE_SPEED.value() - 1 : 0);
        double range = rangeMultiplier(golem);
        var followRange = golem.getAttribute(Attributes.FOLLOW_RANGE);
        boolean changedRange = followRange != null && followRange.hasModifier(RANGE);
        updateModifier(golem, Attributes.FOLLOW_RANGE, RANGE, range - 1);
        if (range != 1 || changedRange) {
            golem.getNavigation().setRequiredPathLength((float) (48 * range));
        }
        if (active) {
            if (golem.tickCount % 5 == 0) {
                level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFFFF0000),
                    golem.getX(), golem.getY() + 0.6, golem.getZ(), 2, 0.3, 0.4, 0.3, 0);
            }
            int fuel = golem.getAttachedOrElse(FUEL, 0) - 1;
            if (fuel == 0) {
                golem.removeAttached(FUEL);
            } else {
                golem.setAttached(FUEL, fuel);
            }
        }
        promoteReserve(golem);
    }

    private static void updateModifier(CopperGolem golem, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, Identifier id, double amount) {
        var instance = golem.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0) {
            instance.removeModifier(id);
        } else if (instance.getModifier(id) == null || instance.getModifier(id).amount() != amount) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void promoteReserve(CopperGolem golem) {
        if (golem.getMainHandItem().isEmpty() && !golem.getOffhandItem().isEmpty()
            && golem.getAttachedOrElse(UPGRADES, Upgrades.NONE).bundle()) {
            golem.setItemSlot(EquipmentSlot.MAINHAND, golem.getOffhandItem());
            golem.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        }
    }

    public static void pickUp(CopperGolem golem, Container container) {
        if (!golem.getMainHandItem().isEmpty() || !golem.getOffhandItem().isEmpty()) {
            return;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean unstackable = stack.getMaxStackSize() == 1;
            int budget = unstackable ? 2 : 32;
            ItemStack main = container.removeItem(slot, Math.min(budget, stack.getMaxStackSize()));
            golem.setItemSlot(EquipmentSlot.MAINHAND, main);
            budget -= main.getCount();
            for (int extraSlot = 0; extraSlot < container.getContainerSize() && budget > 0; extraSlot++) {
                ItemStack extra = container.getItem(extraSlot);
                if (extra.isEmpty() || (unstackable ? extra.getMaxStackSize() != 1 : !ItemStack.isSameItemSameComponents(main, extra))) {
                    continue;
                }
                int merge = Math.min(budget, main.getMaxStackSize() - main.getCount());
                if (merge > 0) {
                    ItemStack removed = container.removeItem(extraSlot, merge);
                    main.grow(removed.getCount());
                    budget -= removed.getCount();
                }
                extra = container.getItem(extraSlot);
                if (budget > 0 && !extra.isEmpty() && golem.getOffhandItem().isEmpty()) {
                    ItemStack reserve = container.removeItem(extraSlot, Math.min(budget, extra.getMaxStackSize()));
                    golem.setItemSlot(EquipmentSlot.OFFHAND, reserve);
                    budget -= reserve.getCount();
                }
            }
            golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            golem.setGuaranteedDrop(EquipmentSlot.OFFHAND);
            container.setChanged();
            return;
        }
    }
}
