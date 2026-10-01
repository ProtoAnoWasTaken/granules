package com.protoano.granules.tea;

import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public enum TeaKind {
    MELON_JUICE(0xFFC44F, false, true, effect(MobEffects.REGENERATION, 30, 0)),
    APPLE_JUICE(0xDBBF4F, false, true, effect(MobEffects.SPEED, 30, 0)),
    GREEN_TEA(0xDFCA62, true, false, effect(MobEffects.SATURATION, 60, 0), effect(MobEffects.JUMP_BOOST, 60, 0)),
    WHITE_TEA(0xCDAF12, true, false, effect(MobEffects.ABSORPTION, 60, 1), effect(MobEffects.STRENGTH, 60, 0)),
    YELLOW_TEA(0xC38B0D, true, false, effect(MobEffects.LUCK, 60, 0), effect(MobEffects.NIGHT_VISION, 60, 0)),
    OOLONG_TEA(0xB86707, true, false, effect(MobEffects.REGENERATION, 60, 0), effect(MobEffects.HASTE, 60, 0)),
    EARL_GREY_TEA(0x370E11, true, false, effect(MobEffects.SPEED, 60, 0), effect(MobEffects.FIRE_RESISTANCE, 60, 0)),
    CHAMOMILE_TEA(0xC96A0A, false, false, effect(MobEffects.BLINDNESS, 30, 0), effect(MobEffects.NAUSEA, 30, 0)),
    DESERT_TEA(0xE880CA, true, false, effect(MobEffects.RESISTANCE, 120, 1), effect(MobEffects.STRENGTH, 120, 0));

    private final int color;
    private final boolean cleansing;
    private final boolean juice;
    private final List<MobEffectInstance> effects;

    TeaKind(int color, boolean cleansing, boolean juice, MobEffectInstance... effects) {
        this.color = color;
        this.cleansing = cleansing;
        this.juice = juice;
        this.effects = List.of(effects);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int color() {
        return color;
    }

    public boolean cleansing() {
        return cleansing;
    }

    public boolean juice() {
        return juice;
    }

    public List<MobEffectInstance> effects() {
        int seconds = com.protoano.granules.config.BalanceConfig.Setting.valueOf(name() + "_SECONDS").intValue();
        int bonus = com.protoano.granules.config.BalanceConfig.Setting.TEA_EFFECT_LEVEL_BONUS.intValue();
        return effects.stream().map(effect -> new MobEffectInstance(effect.getEffect(), seconds * 20,
            effect.getAmplifier() + bonus)).toList();
    }

    private static MobEffectInstance effect(Holder<MobEffect> effect, int seconds, int amplifier) {
        return new MobEffectInstance(effect, seconds * 20, amplifier);
    }
}
