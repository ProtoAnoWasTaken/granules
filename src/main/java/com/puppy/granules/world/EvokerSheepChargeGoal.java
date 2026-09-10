package com.puppy.granules.world;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;

public final class EvokerSheepChargeGoal extends Goal {
	private static final double TARGET_RANGE = 12.0;
	private static final double MAX_CHARGE_RANGE = 18.0;
	private static final double CHARGE_SPEED = 1.65;
	private static final int CHARGE_COOLDOWN = 60;
	private final Sheep sheep;
	private Player target;
	private int cooldown;
	private boolean charged;

	public EvokerSheepChargeGoal(Sheep sheep) {
		this.sheep = sheep;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (cooldown > 0) {
			cooldown--;
			return false;
		}

		if (!((EvokerConvertedSheepAccess) sheep).granules$isEvokerAggressive()) {
			return false;
		}

		Player player = sheep.level().getNearestPlayer(sheep.getX(), sheep.getY(), sheep.getZ(), TARGET_RANGE, true);
		if (player == null || !player.isAlive()) {
			return false;
		}

		target = player;
		charged = false;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return !charged
			&& target != null
			&& target.isAlive()
			&& ((EvokerConvertedSheepAccess) sheep).granules$isEvokerAggressive()
			&& sheep.distanceToSqr(target) <= MAX_CHARGE_RANGE * MAX_CHARGE_RANGE;
	}

	@Override
	public void start() {
		sheep.setAggressive(true);
	}

	@Override
	public void stop() {
		sheep.getNavigation().stop();
		sheep.setAggressive(false);
		target = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}

		sheep.getLookControl().setLookAt(target, 30.0F, 30.0F);
		sheep.getNavigation().moveTo(target, CHARGE_SPEED);
		if (sheep.distanceToSqr(target) > 2.25) {
			return;
		}

		ServerLevel level = (ServerLevel) sheep.level();
		float damage = level.getDifficulty() == Difficulty.HARD ? 3.0F : 2.0F;
		double knockback = level.getDifficulty() == Difficulty.HARD ? 1.1 : 0.8;
		DamageSource source = sheep.damageSources().mobAttack(sheep);
		if (target.hurtServer(level, source, damage)) {
			target.knockback(knockback, sheep.getX() - target.getX(), sheep.getZ() - target.getZ(), source, damage);
		}

		charged = true;
		cooldown = CHARGE_COOLDOWN;
	}
}
