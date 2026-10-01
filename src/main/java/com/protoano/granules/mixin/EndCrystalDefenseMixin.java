package com.protoano.granules.mixin;

import com.protoano.granules.crystal.CrystalDefense;
import com.protoano.granules.crystal.CrystalDefenseAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystal.class)
public abstract class EndCrystalDefenseMixin implements CrystalDefenseAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> GRANULES_DEFENSE = SynchedEntityData.defineId(EndCrystal.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private final CrystalDefense granules$defense = new CrystalDefense();

    @Override
    public CrystalDefense granules$defense() {
        return granules$defense;
    }

    @Override
    public boolean granules$isDefense() {
        return ((EndCrystal) (Object) this).getEntityData().get(GRANULES_DEFENSE);
    }

    @Override
    public void granules$setDefense(boolean enabled) {
        ((EndCrystal) (Object) this).getEntityData().set(GRANULES_DEFENSE, enabled);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void granules$defineDefense(SynchedEntityData.Builder builder, CallbackInfo callback) {
        builder.define(GRANULES_DEFENSE, false);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void granules$tickDefense(CallbackInfo callback) {
        granules$defense.tick((EndCrystal) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void granules$saveDefense(ValueOutput output, CallbackInfo callback) {
        granules$defense.save(output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void granules$loadDefense(ValueInput input, CallbackInfo callback) {
        granules$defense.load(input);
        granules$setDefense(granules$defense.enabled());
    }
}
