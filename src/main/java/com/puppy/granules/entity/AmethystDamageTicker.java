package com.puppy.granules.entity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AmethystDamageTicker {
	private static final List<DelayedDamage> PENDING_DAMAGE = new ArrayList<>();

	private AmethystDamageTicker() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
	}

	public static void schedule(LivingEntity target, float damage) {
		if (damage > 0.0F) {
			PENDING_DAMAGE.add(new DelayedDamage(target, damage, 4, 20));
		}
	}

	private static void tick() {
		Iterator<DelayedDamage> iterator = PENDING_DAMAGE.iterator();
		while (iterator.hasNext()) {
			DelayedDamage delayedDamage = iterator.next();
			if (!delayedDamage.target().isAlive()) {
				iterator.remove();
				continue;
			}
			if (delayedDamage.ticksUntilDamage() > 1) {
				delayedDamage.setTicksUntilDamage(delayedDamage.ticksUntilDamage() - 1);
				continue;
			}
			LivingEntity target = delayedDamage.target();
			target.hurtOrSimulate(target.damageSources().magic(), delayedDamage.damage());
			if (target.level() instanceof ServerLevel level) {
				level.playSound(
					null,
					target.getX(),
					target.getY(),
					target.getZ(),
					SoundEvents.AMETHYST_BLOCK_BREAK,
					SoundSource.PLAYERS,
					0.65F,
					0.5F + level.getRandom().nextFloat() * 0.7F
				);
				level.sendParticles(
					ParticleTypes.CRIT,
					target.getX(),
					target.getY(0.5D),
					target.getZ(),
					1 + level.getRandom().nextInt(2),
					target.getBbWidth() * 0.25D,
					target.getBbHeight() * 0.25D,
					target.getBbWidth() * 0.25D,
					0.05D
				);
			}
			if (delayedDamage.remainingHits() == 1) {
				iterator.remove();
			} else {
				delayedDamage.setRemainingHits(delayedDamage.remainingHits() - 1);
				delayedDamage.setTicksUntilDamage(20);
			}
		}
	}

	private static final class DelayedDamage {
		private final LivingEntity target;
		private final float damage;
		private int remainingHits;
		private int ticksUntilDamage;

		private DelayedDamage(LivingEntity target, float damage, int remainingHits, int ticksUntilDamage) {
			this.target = target;
			this.damage = damage;
			this.remainingHits = remainingHits;
			this.ticksUntilDamage = ticksUntilDamage;
		}

		private LivingEntity target() {
			return this.target;
		}

		private float damage() {
			return this.damage;
		}

		private int remainingHits() {
			return this.remainingHits;
		}

		private void setRemainingHits(int remainingHits) {
			this.remainingHits = remainingHits;
		}

		private int ticksUntilDamage() {
			return this.ticksUntilDamage;
		}

		private void setTicksUntilDamage(int ticksUntilDamage) {
			this.ticksUntilDamage = ticksUntilDamage;
		}
	}
}
