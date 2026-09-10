package com.puppy.granules.bomb;

import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BombProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> PLANTED = SynchedEntityData.defineId(BombProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FULLY_CHARGED = SynchedEntityData.defineId(BombProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(BombProjectile.class, EntityDataSerializers.INT);

    public BombProjectile(EntityType<? extends BombProjectile> type, Level level) {
        super(type, level);
    }

    public BombProjectile(Level level, LivingEntity owner, ItemStack stack) {
        super(BombContent.BOMB_ENTITY, owner, level, stack);
    }

    public BombProjectile(Level level, double x, double y, double z, ItemStack stack) {
        super(BombContent.BOMB_ENTITY, x, y, z, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return BombContent.DYNAMITE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLANTED, false);
        builder.define(FULLY_CHARGED, false);
        builder.define(FUSE, 80);
    }

    @Override
    public void tick() {
        if (!this.entityData.get(PLANTED)) {
            super.tick();
            if (this.entityData.get(FULLY_CHARGED) && this.level().isClientSide()) {
                this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        int fuse = this.entityData.get(FUSE) - 1;
        this.entityData.set(FUSE, fuse);
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.15, this.getZ(), 0.0, 0.0, 0.0);
        }
        if (fuse <= 0 && this.level() instanceof ServerLevel serverLevel) {
            this.detonate(serverLevel);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (this.entityData.get(PLANTED)) {
            return;
        }
        Vec3 normal = new Vec3(
            hit.getDirection().getStepX(),
            hit.getDirection().getStepY(),
            hit.getDirection().getStepZ()
        ).scale(0.08);
        this.setPos(hit.getLocation().add(normal));
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.entityData.set(PLANTED, true);
        this.entityData.set(FUSE, 80);
        this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
    }

    private void detonate(ServerLevel level) {
        Item item = this.getItem().getItem();
        float power = item == BombContent.DRY_BOMB ? 4.0F : 2.0F;
        Level.ExplosionInteraction interaction = item == BombContent.DYNAMITE
            ? Level.ExplosionInteraction.TNT
            : Level.ExplosionInteraction.NONE;
        level.explode(this, this.getX(), this.getY(), this.getZ(), power, interaction);
        if (item == BombContent.DIRT_BOMB) {
            this.replaceWithDirt(level, 4);
        }
        if (item == BombContent.DRY_BOMB) {
            this.dryLiquids(level, 8);
        }
    }

    private void replaceWithDirt(ServerLevel level, int radius) {
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (center.distSqr(pos) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null && !PoiTypes.hasPoi(state)) {
                level.setBlockAndUpdate(pos.immutable(), Blocks.DIRT.defaultBlockState());
            }
        }
    }

	private void dryLiquids(ServerLevel level, int radius) {
		BlockPos center = this.blockPosition();
		boolean removedLiquid = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (center.distSqr(pos) > radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().isEmpty()) {
                continue;
            }
			if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
				level.setBlockAndUpdate(pos.immutable(), state.setValue(BlockStateProperties.WATERLOGGED, false));
			} else {
				level.setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
			}
			removedLiquid = true;
		}
		if (removedLiquid && this.getOwner() instanceof net.minecraft.server.level.ServerPlayer player) {
			GranulesAdvancements.award(player, "sponge_bomb_square_pit");
		}
    }

    public void setFullyCharged(boolean fullyCharged) {
        this.entityData.set(FULLY_CHARGED, fullyCharged);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("planted", this.entityData.get(PLANTED));
        output.putBoolean("fully_charged", this.entityData.get(FULLY_CHARGED));
        output.putInt("fuse", this.entityData.get(FUSE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean planted = input.getBooleanOr("planted", false);
        this.entityData.set(PLANTED, planted);
        this.entityData.set(FULLY_CHARGED, input.getBooleanOr("fully_charged", false));
        this.entityData.set(FUSE, input.getIntOr("fuse", 80));
        this.setNoGravity(planted);
    }
}
