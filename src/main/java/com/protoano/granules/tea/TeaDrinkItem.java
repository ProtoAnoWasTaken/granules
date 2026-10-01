package com.protoano.granules.tea;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class TeaDrinkItem extends Item {
    private final TeaKind kind;

    public TeaDrinkItem(TeaKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public TeaKind kind() {
        return kind;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            if (kind.cleansing()) {
                entity.removeAllEffects();
            }
            if (kind == TeaKind.CHAMOMILE_TEA) {
                tabulaRasa(entity);
            }
            for (var effect : kind.effects()) {
                entity.addEffect(effect);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    private static void tabulaRasa(LivingEntity entity) {
        entity.removeEffect(MobEffects.BAD_OMEN);
        entity.removeEffect(MobEffects.RAID_OMEN);
        entity.removeEffect(MobEffects.TRIAL_OMEN);
        if (entity instanceof ServerPlayer player) {
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            player.getWardenSpawnTracker().ifPresent(tracker -> {
                tracker.reset();
            });
            for (var piglin : player.level().getEntitiesOfClass(ZombifiedPiglin.class, player.getBoundingBox().inflate(com.protoano.granules.config.BalanceConfig.Setting.CHAMOMILE_PARDON_RADIUS.value()))) {
                var target = piglin.getPersistentAngerTarget();
                if (piglin.getTarget() == player || target != null && target.matches(player)) {
                    piglin.stopBeingAngry();
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        for (var effect : kind.effects()) {
            var name = PotionContents.getPotionDescription(effect.getEffect(), effect.getAmplifier());
            tooltip.accept(Component.translatable("potion.withDuration", name, MobEffectUtil.formatDuration(effect, 1.0F, context.tickRate()))
                .withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
        }
        if (kind == TeaKind.CHAMOMILE_TEA) {
            tooltip.accept(Component.translatable("effect.granules.tabula_rasa").withStyle(ChatFormatting.BLUE));
        }
        if (kind.cleansing()) {
            tooltip.accept(Component.translatable("effect.granules.instant_cleansing").withStyle(ChatFormatting.BLUE));
        }
    }
}
