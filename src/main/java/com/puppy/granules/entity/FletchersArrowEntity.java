package com.puppy.granules.entity;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.fletching.ArrowParts;
import com.puppy.granules.fletching.FletchersArrowItem;
import com.puppy.granules.mixin.AbstractArrowAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FletchersArrowEntity extends AbstractArrow {
	private static final EntityDataAccessor<Integer> SHAFT = SynchedEntityData.defineId(FletchersArrowEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FLETCH = SynchedEntityData.defineId(FletchersArrowEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HEAD = SynchedEntityData.defineId(FletchersArrowEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ECHO_PHANTOM = SynchedEntityData.defineId(FletchersArrowEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> FIREWORK_FLIGHT_TICKS = SynchedEntityData.defineId(FletchersArrowEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FIREWORK_FLIGHT_AGE = SynchedEntityData.defineId(FletchersArrowEntity.class, EntityDataSerializers.INT);
	private ArrowParts parts = new ArrowParts(ArrowParts.Shaft.STICK, ArrowParts.Fletch.FEATHER, ArrowParts.Head.FLINT);
	private boolean echoSoundEmitted;
	private boolean spawnedEchoPhantom;
	private int echoPhantomId = -1;
	private int echoDelay;
	private boolean hitMob;
	private boolean retracting;
	private boolean fireworkLaunchSoundPlayed;
	private double fletchersBaseDamage = 2.0D;

	public FletchersArrowEntity(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
	}

	public FletchersArrowEntity(
		Level level,
		LivingEntity owner,
		ItemStack pickupItem,
		@Nullable ItemStack firedFromWeapon,
		ArrowParts parts
	) {
		super(GranulesMod.FLETCHERS_ARROW_ENTITY, owner, level, pickupItem, firedFromWeapon);
		this.setParts(parts);
		this.applyParts();
	}

	public FletchersArrowEntity(
		Level level,
		double x,
		double y,
		double z,
		ItemStack pickupItem,
		@Nullable ItemStack firedFromWeapon,
		ArrowParts parts
	) {
		super(GranulesMod.FLETCHERS_ARROW_ENTITY, x, y, z, level, pickupItem, firedFromWeapon);
		this.setParts(parts);
		this.applyParts();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHAFT, ArrowParts.Shaft.STICK.ordinal());
		builder.define(FLETCH, ArrowParts.Fletch.FEATHER.ordinal());
		builder.define(HEAD, ArrowParts.Head.FLINT.ordinal());
		builder.define(ECHO_PHANTOM, false);
		builder.define(FIREWORK_FLIGHT_TICKS, 0);
		builder.define(FIREWORK_FLIGHT_AGE, 0);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		ArrowParts currentParts = this.parts;
		if (currentParts == null) {
			currentParts = new ArrowParts(ArrowParts.Shaft.STICK, ArrowParts.Fletch.FEATHER, ArrowParts.Head.FLINT);
		}
		return FletchersArrowItem.createStack(currentParts, 1);
	}

	@Override
	protected float getWaterInertia() {
		if (this.parts().head() == ArrowParts.Head.PRISMARINE) {
			return 0.99F;
		}
		return super.getWaterInertia();
	}

	@Override
	protected double getDefaultGravity() {
		if (this.parts().shaft() == ArrowParts.Shaft.BREEZE_ROD) {
			return 0.025D;
		}
		return super.getDefaultGravity();
	}

	@Override
	protected float getAirDrag() {
		if (this.isFireworkFlightActive()) {
			return 1.0F;
		}
		if (this.parts().shaft() == ArrowParts.Shaft.BLAZE_ROD) {
			return 0.94F;
		}
		return super.getAirDrag();
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		Entity target = hitResult.getEntity();
		this.hitMob = true;
		Component arrowName = this.getCustomName();
		float impactDamage = (float) Math.ceil(this.getDeltaMovement().length() * this.fletchersBaseDamage);
		super.onHitEntity(hitResult);
		if (this.level().isClientSide()) {
			return;
		}
		this.assignTargetToOwner(target);
		if (this.parts().head() == ArrowParts.Head.COAL) {
			target.igniteForSeconds(5.0F);
		}
		if (this.parts().fletch() == ArrowParts.Fletch.NAME_TAG && arrowName != null && target.isAlive() && !target.hasCustomName()) {
			target.setCustomName(arrowName);
		}
		if (this.parts().shaft() == ArrowParts.Shaft.BONE && !target.isAlive() && this.random.nextBoolean()) {
			this.spawnAtLocation((ServerLevel) this.level(), new ItemStack(net.minecraft.world.item.Items.BONE));
		}
		if (this.parts().head() == ArrowParts.Head.AMETHYST && target instanceof LivingEntity livingTarget) {
			AmethystDamageTicker.schedule(livingTarget, impactDamage * 0.125F);
		}
		this.weakenEchoPhantom();
	}

	@Override
	protected void onHitBlock(BlockHitResult hitResult) {
		if (this.isEchoPhantom()) {
			if (!this.level().isClientSide()) {
				this.discard();
			}
			return;
		}
		super.onHitBlock(hitResult);
		if (!this.level().isClientSide() && this.parts().head() == ArrowParts.Head.AMETHYST) {
			this.discard();
		}
	}

	@Override
	public void tick() {
		if (this.isEchoPhantom() && this.echoDelay > 0) {
			this.echoDelay--;
			return;
		}
		if (this.retracting) {
			this.tickRetraction();
			return;
		}
		if (!this.level().isClientSide() && this.shouldSeverStringLine()) {
			this.setOwner((Entity) null);
		}
		if (!this.level().isClientSide() && this.parts().head() == ArrowParts.Head.ECHO && !this.isEchoPhantom() && !this.spawnedEchoPhantom) {
			this.spawnEchoPhantom();
		}
		boolean fireworkFlightActive = this.isFireworkFlightActive();
		if (this.parts().fletch() == ArrowParts.Fletch.FIREWORK) {
			this.setNoGravity(fireworkFlightActive);
			this.playFireworkLaunchSound();
		}
		super.tick();
		if (!this.level().isClientSide() && fireworkFlightActive) {
			this.entityData.set(FIREWORK_FLIGHT_AGE, this.entityData.get(FIREWORK_FLIGHT_AGE) + 1);
		}
		if (this.level().isClientSide() && fireworkFlightActive && this.tickCount % 2 == 0) {
			this.level().addParticle(
				ParticleTypes.FIREWORK,
				this.getX(),
				this.getY(),
				this.getZ(),
				this.random.nextGaussian() * 0.05D,
				-this.getDeltaMovement().y * 0.5D,
				this.random.nextGaussian() * 0.05D
			);
		}
		if (this.level().isClientSide() && this.parts().shaft() == ArrowParts.Shaft.BLAZE_ROD && this.tickCount % 2 == 0) {
			this.addShaftTrailParticle(ParticleTypes.FLAME, 0.01D);
		}
		if (this.level().isClientSide() && this.parts().shaft() == ArrowParts.Shaft.BREEZE_ROD && this.tickCount % 3 == 0) {
			this.addShaftTrailParticle(ParticleTypes.SMALL_GUST, 0.0D);
		}
		if (this.level().isClientSide() && this.parts().shaft() == ArrowParts.Shaft.END_ROD && this.tickCount % 2 == 0) {
			this.addShaftTrailParticle(ParticleTypes.END_ROD, 0.0D);
		}
		if (!this.level().isClientSide() && !this.echoSoundEmitted && this.parts().head() == ArrowParts.Head.ECHO) {
			this.gameEvent(GranulesMod.ECHO_ARROW_SHOOT, this.getOwner());
			this.echoSoundEmitted = true;
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setParts(FletchersArrowItem.partsOf(this.getPickupItemStackOrigin()));
		this.applyParts();
	}

	public ArrowParts parts() {
		ArrowParts.Shaft shaft = ArrowParts.Shaft.values()[this.entityData.get(SHAFT)];
		ArrowParts.Fletch fletch = ArrowParts.Fletch.values()[this.entityData.get(FLETCH)];
		ArrowParts.Head head = ArrowParts.Head.values()[this.entityData.get(HEAD)];
		return new ArrowParts(shaft, fletch, head);
	}

	public boolean isEchoPhantom() {
		return this.entityData.get(ECHO_PHANTOM);
	}

	public static boolean reelStringArrows(Player player) {
		boolean startedRetraction = false;
		for (FletchersArrowEntity arrow : player.level().getEntities(
			EntityTypeTest.forClass(FletchersArrowEntity.class),
			player.getBoundingBox().inflate(64.0D),
			candidate -> candidate.isActiveStringLineFor(player)
		)) {
			if (!arrow.retracting) {
				arrow.retracting = true;
				startedRetraction = true;
			}
		}
		if (startedRetraction) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		return startedRetraction;
	}

	public static boolean hasActiveStringArrow(Player player) {
		return !player.level().getEntities(
			EntityTypeTest.forClass(FletchersArrowEntity.class),
			player.getBoundingBox().inflate(64.0D),
			candidate -> candidate.isActiveStringLineFor(player)
		).isEmpty();
	}

	private boolean isActiveStringLineFor(Player player) {
		return !this.hitMob
			&& this.parts().fletch() == ArrowParts.Fletch.STRING
			&& this.getOwner() == player;
	}

	private void tickRetraction() {
		Entity owner = this.getOwner();
		if (!(owner instanceof Player player) || !player.isAlive()) {
			this.retracting = false;
			return;
		}
		Vec3 destination = player.getEyePosition();
		Vec3 direction = destination.subtract(this.position());
		if (direction.lengthSqr() < 2.25D) {
			if (!player.getInventory().add(this.getPickupItem())) {
				this.spawnAtLocation((ServerLevel) this.level(), this.getPickupItem());
			}
			this.discard();
			return;
		}
		this.setNoGravity(true);
		this.setDeltaMovement(direction.normalize().scale(1.5D));
		this.setPos(this.position().add(this.getDeltaMovement()));
	}

	private void spawnEchoPhantom() {
		FletchersArrowEntity phantom = new FletchersArrowEntity(
			this.level(),
			this.getX(),
			this.getY(),
			this.getZ(),
			this.getPickupItemStackOrigin().copy(),
			this.getWeaponItem(),
			this.parts()
		);
		phantom.setOwner(this.getOwner());
		phantom.setDeltaMovement(this.getDeltaMovement());
		phantom.setYRot(this.getYRot());
		phantom.setXRot(this.getXRot());
		phantom.entityData.set(ECHO_PHANTOM, true);
		phantom.echoDelay = 8;
		phantom.pickup = Pickup.DISALLOWED;
		phantom.fletchersBaseDamage = this.fletchersBaseDamage * 1.5D;
		phantom.setBaseDamage(phantom.fletchersBaseDamage);
		this.level().addFreshEntity(phantom);
		this.echoPhantomId = phantom.getId();
		this.spawnedEchoPhantom = true;
	}

	private void weakenEchoPhantom() {
		if (this.echoPhantomId < 0) {
			return;
		}
		Entity entity = this.level().getEntity(this.echoPhantomId);
		if (entity instanceof FletchersArrowEntity phantom && phantom.isEchoPhantom()) {
			phantom.fletchersBaseDamage = this.fletchersBaseDamage * 0.5D;
			phantom.setBaseDamage(phantom.fletchersBaseDamage);
		}
	}

	private void setParts(ArrowParts parts) {
		this.parts = parts;
		this.entityData.set(SHAFT, parts.shaft().ordinal());
		this.entityData.set(FLETCH, parts.fletch().ordinal());
		this.entityData.set(HEAD, parts.head().ordinal());
	}

	private void applyParts() {
		ArrowParts currentParts = this.parts();
		double damage = switch (currentParts.head()) {
			case QUARTZ -> 1.5D;
			case AMETHYST, GHAST -> 1.5D;
			case PRISMARINE -> 2.5D;
			case COAL -> 0.5D;
			default -> 2.0D;
		};
		if (currentParts.head() == ArrowParts.Head.COAL && (
			currentParts.shaft() == ArrowParts.Shaft.BLAZE_ROD || currentParts.fletch() == ArrowParts.Fletch.FIREWORK
		)) {
			damage *= 1.5D;
		}
		this.setBaseDamage(damage);
		this.fletchersBaseDamage = damage;
		if (currentParts.head() == ArrowParts.Head.QUARTZ) {
			((AbstractArrowAccessor) this).granules$setPierceLevel((byte) 1);
		}
		if (currentParts.fletch() == ArrowParts.Fletch.NAME_TAG) {
			Component customName = this.getPickupItemStackOrigin().get(DataComponents.CUSTOM_NAME);
			if (customName != null) {
				this.setCustomName(customName);
			}
		}
		this.initializeFireworkFlight();
	}

	private void assignTargetToOwner(Entity target) {
		if (target instanceof Mob mob && this.getOwner() instanceof Player player && !player.getAbilities().instabuild) {
			mob.setTarget(player);
		}
	}

	private boolean isFireworkFlightActive() {
		return this.parts().fletch() == ArrowParts.Fletch.FIREWORK
			&& this.entityData.get(FIREWORK_FLIGHT_AGE) < this.entityData.get(FIREWORK_FLIGHT_TICKS);
	}

	private void initializeFireworkFlight() {
		if (this.level().isClientSide() || this.parts().fletch() != ArrowParts.Fletch.FIREWORK || this.entityData.get(FIREWORK_FLIGHT_TICKS) > 0) {
			return;
		}
		Fireworks fireworks = this.getPickupItemStackOrigin().get(DataComponents.FIREWORKS);
		int flightDuration = fireworks != null ? fireworks.flightDuration() : 0;
		int flightTicks = 10 * (1 + flightDuration) + this.random.nextInt(6) + this.random.nextInt(7);
		this.entityData.set(FIREWORK_FLIGHT_TICKS, flightTicks);
	}

	private void playFireworkLaunchSound() {
		if (this.level().isClientSide() || this.fireworkLaunchSoundPlayed) {
			return;
		}
		SoundSource soundSource = this.getOwner() != null ? this.getOwner().getSoundSource() : SoundSource.BLOCKS;
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, soundSource, 3.0F, 1.0F);
		this.fireworkLaunchSoundPlayed = true;
	}

	private boolean shouldSeverStringLine() {
		return this.parts().fletch() == ArrowParts.Fletch.STRING
			&& !this.hitMob
			&& this.getOwner() instanceof Player player
			&& !isStringArrowWeapon(player.getMainHandItem());
	}

	private boolean isStringArrowWeapon(ItemStack stack) {
		return stack.is(net.minecraft.world.item.Items.BOW) || stack.is(net.minecraft.world.item.Items.CROSSBOW);
	}

	private void addShaftTrailParticle(net.minecraft.core.particles.ParticleOptions particle, double upwardMotion) {
		Vec3 trailPosition = this.position().subtract(this.getDeltaMovement().scale(0.2D));
		this.level().addParticle(
			particle,
			trailPosition.x,
			trailPosition.y,
			trailPosition.z,
			this.random.nextGaussian() * 0.01D,
			upwardMotion,
			this.random.nextGaussian() * 0.01D
		);
	}
}
