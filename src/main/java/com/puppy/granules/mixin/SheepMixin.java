package com.puppy.granules.mixin;

import com.puppy.granules.world.GlowSheepAccess;
import com.puppy.granules.world.EvokerConvertedSheepAccess;
import com.puppy.granules.world.EvokerSheepChargeGoal;
import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public abstract class SheepMixin implements GlowSheepAccess, EvokerConvertedSheepAccess {
	@Shadow
	@Final
	private static EntityDataAccessor<Byte> DATA_WOOL_ID;

	@Shadow
	public abstract boolean isSheared();

	@Shadow
	public abstract net.minecraft.world.item.DyeColor getColor();

	@Unique
	private boolean granules$evokerConverted;

	@Override
	public boolean granules$hasGlowWool() {
		SynchedEntityData entityData = ((com.puppy.granules.mixin.EntityDataAccessor) (Object) this).granules$getEntityData();
		return (entityData.get(DATA_WOOL_ID) & 32) != 0;
	}

	@Override
	public void granules$setGlowWool(boolean hasGlowWool) {
		SynchedEntityData entityData = ((com.puppy.granules.mixin.EntityDataAccessor) (Object) this).granules$getEntityData();
		byte woolData = entityData.get(DATA_WOOL_ID);
		byte updatedWoolData = hasGlowWool
			? (byte) (woolData | 32)
			: (byte) (woolData & ~32);
		entityData.set(DATA_WOOL_ID, updatedWoolData);
	}

	@Override
	public boolean granules$isEvokerConverted() {
		return granules$evokerConverted;
	}

	@Override
	public void granules$setEvokerConverted(boolean evokerConverted) {
		granules$evokerConverted = evokerConverted;
	}

	@Override
	public boolean granules$isEvokerAggressive() {
		DyeColor color = getColor();
		return granules$evokerConverted && (color == DyeColor.RED || color == DyeColor.BLUE);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void granules$addEvokerSheepChargeGoal(CallbackInfo callbackInfo) {
		Sheep sheep = (Sheep) (Object) this;
		sheep.getGoalSelector().addGoal(1, new EvokerSheepChargeGoal(sheep));
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void granules$interactWithGlowWool(
		Player player,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Items.GLOW_INK_SAC) && !isSheared() && !granules$hasGlowWool()) {
			if (!player.level().isClientSide()) {
				granules$setGlowWool(true);
				if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					GranulesAdvancements.award(serverPlayer, "electric_sheep");
				}
				granules$spawnGlowInkParticles((ServerLevel) player.level());
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
			}
			callbackInfo.setReturnValue(InteractionResult.SUCCESS_SERVER);
			return;
		}
		if (stack.is(ItemTags.AXES) && granules$hasGlowWool()) {
			if (!player.level().isClientSide()) {
				granules$setGlowWool(false);
				granules$spawnGlowInkParticles((ServerLevel) player.level());
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				((Sheep) (Object) this).gameEvent(GameEvent.SHEAR, player);
			}
			callbackInfo.setReturnValue(InteractionResult.SUCCESS_SERVER);
		}
	}

	@Unique
	private void granules$spawnGlowInkParticles(ServerLevel level) {
		Sheep sheep = (Sheep) (Object) this;
		level.sendParticles(ParticleTypes.GLOW_SQUID_INK, sheep.getX(), sheep.getY() + 0.8, sheep.getZ(), 12, 0.35, 0.45, 0.35, 0.03);
	}

	@Inject(method = "shear", at = @At("TAIL"))
	private void granules$occasionallyRemoveGlowWool(ServerLevel level, net.minecraft.sounds.SoundSource soundSource, ItemStack shears, CallbackInfo callbackInfo) {
		if (granules$hasGlowWool() && level.getRandom().nextInt(4) == 0) {
			granules$setGlowWool(false);
		}
	}

	@Inject(
		method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/animal/sheep/Sheep;",
		at = @At("RETURN")
	)
	private void granules$inheritGlowWool(ServerLevel level, AgeableMob otherParent, CallbackInfoReturnable<Sheep> callbackInfo) {
		Sheep offspring = callbackInfo.getReturnValue();
		if (offspring == null) {
			return;
		}
		boolean firstParentGlows = granules$hasGlowWool();
		boolean secondParentGlows = otherParent instanceof Sheep sheep && ((GlowSheepAccess) sheep).granules$hasGlowWool();
		if (firstParentGlows && secondParentGlows || (firstParentGlows || secondParentGlows) && level.getRandom().nextBoolean()) {
			((GlowSheepAccess) offspring).granules$setGlowWool(true);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void granules$saveGlowWool(ValueOutput output, CallbackInfo callbackInfo) {
		output.putBoolean("GranulesGlowWool", granules$hasGlowWool());
		output.putBoolean("GranulesEvokerConverted", granules$evokerConverted);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void granules$loadGlowWool(ValueInput input, CallbackInfo callbackInfo) {
		granules$setGlowWool(input.getBooleanOr("GranulesGlowWool", false));
		granules$evokerConverted = input.getBooleanOr("GranulesEvokerConverted", false);
	}
}
