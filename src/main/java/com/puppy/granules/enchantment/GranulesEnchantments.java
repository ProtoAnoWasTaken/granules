package com.puppy.granules.enchantment;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.rabbit.PalePeltEquipment;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class GranulesEnchantments {
    public static final ResourceKey<Enchantment> NECROSOLES = key("necrosoles");
    public static final ResourceKey<Enchantment> HIGH_STEP = key("high_step");
    public static final ResourceKey<Enchantment> BEHEADING = key("beheading");
    public static final ResourceKey<Enchantment> REACH = key("reach");
    public static final ResourceKey<Enchantment> VAMPIRISM = key("vampirism");
    public static final ResourceKey<Enchantment> FELLING = key("felling");
    public static final ResourceKey<Enchantment> RADIUS = key("radius");
    public static final ResourceKey<Enchantment> CURSE_OF_PIERCING = key("curse_of_piercing");
    public static final ResourceKey<Enchantment> CURSE_OF_CORROSION = key("curse_of_corrosion");
    public static final ResourceKey<Enchantment> CURSE_OF_COMBUSTION = key("curse_of_combustion");
    public static final ResourceKey<Enchantment> CURSE_OF_CRUMPLING = key("curse_of_crumpling");
    public static final ResourceKey<Enchantment> CURSE_OF_SLACK = key("curse_of_slack");
    private static final Identifier STEP_MODIFIER = id("high_step");
    private static final Identifier BLOCK_REACH_MODIFIER = id("block_reach");
    private static final Identifier ENTITY_REACH_MODIFIER = id("entity_reach");
    private static final Identifier WATER_MOVEMENT_MODIFIER = id("necrosoles_water_movement");
    private static final ThreadLocal<Boolean> EXTRA_BREAKING = ThreadLocal.withInitial(() -> false);

    private GranulesEnchantments() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updatePlayer(player);
            }
        });
    }

    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
    public static int level(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    public static void updatePlayer(ServerPlayer player) {
        ServerLevel level = player.level();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int highStep = level(level, boots, HIGH_STEP);
        applyModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_MODIFIER, highStep, AttributeModifier.Operation.ADD_VALUE);
        ItemStack reachStack = player.getMainHandItem();
        if (player.getOffhandItem().is(PalePeltEquipment.OPERA_GLOVE)) {
            reachStack = player.getOffhandItem();
        }
        int reach = level(level, reachStack, REACH);
        applyModifier(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_REACH_MODIFIER, reach, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_REACH_MODIFIER, reach, AttributeModifier.Operation.ADD_VALUE);
        int necrosoles = level(level, boots, NECROSOLES);
        applyModifier(player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY), WATER_MOVEMENT_MODIFIER, necrosoles, AttributeModifier.Operation.ADD_VALUE);

    }

    public static void breakAdditionalBlocks(ServerPlayer player, BlockPos origin, BlockState original) {
        if (EXTRA_BREAKING.get() || player.isShiftKeyDown()) {
            return;
        }
        ServerLevel level = player.level();
        ItemStack tool = player.getMainHandItem();
        Set<BlockPos> targets = new HashSet<>();
        if (level(level, tool, FELLING) > 0 && original.is(BlockTags.LOGS) && !level.getBlockState(origin.below()).is(BlockTags.LOGS)) {
            collectTree(level, origin, targets);
        }
        int radius = level(level, tool, RADIUS);
        if (radius > 0 && tool.isCorrectToolForDrops(original)) {
            collectRadius(player, origin, radius, targets);
        }
        targets.remove(origin);
        EXTRA_BREAKING.set(true);
        int destroyed = 0;
        try {
            for (BlockPos target : targets) {
                if (player.gameMode.destroyBlock(target)) {
                    destroyed++;
                }
            }
        } finally {
            EXTRA_BREAKING.set(false);
        }
        if (tool.isDamageableItem() && destroyed > 0) {
            int maximumDamage = Math.max(0, tool.getMaxDamage() - 5);
            tool.setDamageValue(Math.min(maximumDamage, tool.getDamageValue() + destroyed));
        }
    }

    public static boolean protectFiveDurability(ItemStack stack) {
        if (!EXTRA_BREAKING.get()) {
            return false;
        }
        return level(stack, FELLING) > 0 || level(stack, RADIUS) > 0;
    }

    public static java.util.List<ItemStack> smeltDrops(ServerLevel level, ItemStack tool, java.util.List<ItemStack> drops) {
        int fireAspect = level(level, tool, net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT);
        if (fireAspect == 0) {
            return drops;
        }
        java.util.List<ItemStack> results = new java.util.ArrayList<>();
        for (ItemStack stack : drops) {
            net.minecraft.world.item.crafting.SingleRecipeInput input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
            java.util.Optional<? extends net.minecraft.world.item.crafting.RecipeHolder<?>> recipe = java.util.Optional.empty();
            if (fireAspect >= 2) {
                recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BLASTING, input, level);
            }
            if (recipe.isEmpty()) {
                recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
            }
            if (recipe.isEmpty()) {
                results.add(stack);
                continue;
            }
            ItemStack output = ((net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.SingleRecipeInput>) recipe.get().value()).assemble(input);
            output.setCount(output.getCount() * stack.getCount());
            results.add(output);
        }
        return results;
    }
    private static void collectTree(ServerLevel level, BlockPos origin, Set<BlockPos> targets) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(origin);
        while (!pending.isEmpty() && targets.size() < 512) {
            BlockPos current = pending.removeFirst();
            if (!targets.add(current)) {
                continue;
            }
            for (BlockPos neighbor : BlockPos.betweenClosed(current.offset(-1, 0, -1), current.offset(1, 1, 1))) {
                BlockPos immutable = neighbor.immutable();
                if (!targets.contains(immutable) && level.getBlockState(immutable).is(BlockTags.LOGS)) {
                    pending.add(immutable);
                }
            }
        }
    }

    private static void collectRadius(ServerPlayer player, BlockPos origin, int radius, Set<BlockPos> targets) {
        Vec3 look = player.getLookAngle();
        Direction direction = player.getDirection();
        if (Math.abs(look.y) > 0.7D) {
            direction = look.y > 0.0D ? Direction.UP : Direction.DOWN;
        }
        for (int depth = 0; depth < radius; depth++) {
            BlockPos center = origin.relative(direction, depth);
            for (int first = -1; first <= 1; first++) {
                for (int second = -1; second <= 1; second++) {
                    if (direction.getAxis() == Direction.Axis.Y) {
                        targets.add(center.offset(first, 0, second));
                    } else if (direction.getAxis() == Direction.Axis.X) {
                        targets.add(center.offset(0, first, second));
                    } else {
                        targets.add(center.offset(first, second, 0));
                    }
                }
            }
        }
    }

    private static void applyModifier(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(id);
        if (amount != 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, path);
    }
}






