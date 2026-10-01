package com.protoano.granules.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record LuckAdjustedChance(float chance) implements LootItemCondition {
    public static final MapCodec<LuckAdjustedChance> CODEC = Codec.floatRange(0.0F, 1.0F)
        .fieldOf("chance").xmap(LuckAdjustedChance::new, LuckAdjustedChance::chance);

    public static void initialize() {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,
            Identifier.fromNamespaceAndPath("granules", "luck_adjusted_chance"), CODEC);
    }

    public static float adjusted(float chance, float luck) {
        return Math.clamp(chance * (1.0F + com.protoano.granules.config.BalanceConfig.Setting.LUCK_MULTIPLIER.floatValue() * luck), 0.0F, 1.0F);
    }

    public static LootItemCondition.Builder randomChance(float chance) {
        return () -> new LuckAdjustedChance(chance);
    }

    @Override
    public MapCodec<LuckAdjustedChance> codec() {
        return CODEC;
    }

    @Override
    public boolean test(LootContext context) {
        return context.getRandom().nextFloat() < adjusted(chance, context.getLuck());
    }
}
