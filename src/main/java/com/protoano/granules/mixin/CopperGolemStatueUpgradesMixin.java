package com.protoano.granules.mixin;

import com.protoano.granules.golem.CopperGolemUpgrades;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolemStatueBlockEntity.class)
public class CopperGolemStatueUpgradesMixin {
    @Inject(method = "createStatue", at = @At("TAIL"))
    private void granules$preserveUpgrades(CopperGolem golem, CallbackInfo callback) {
        var statue = (CopperGolemStatueBlockEntity) (Object) this;
        var tag = statue.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.store("granules_copper_upgrades", CopperGolemUpgrades.Upgrades.CODEC,
            golem.getAttachedOrElse(CopperGolemUpgrades.UPGRADES, CopperGolemUpgrades.Upgrades.NONE));
        tag.putInt("granules_redstone_fuel", golem.getAttachedOrElse(CopperGolemUpgrades.FUEL, 0));
        statue.setComponents(DataComponentMap.builder().addAll(statue.components())
            .set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build());
        statue.setChanged();
    }

    @Inject(method = "removeStatue", at = @At("RETURN"))
    private void granules$restoreUpgrades(BlockState state, CallbackInfoReturnable<CopperGolem> callback) {
        var golem = callback.getReturnValue();
        if (golem == null) {
            return;
        }
        var statue = (CopperGolemStatueBlockEntity) (Object) this;
        var tag = statue.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.read("granules_copper_upgrades", CopperGolemUpgrades.Upgrades.CODEC)
            .ifPresent(upgrades -> golem.setAttached(CopperGolemUpgrades.UPGRADES, upgrades));
        int fuel = Math.clamp(tag.getIntOr("granules_redstone_fuel", 0), 0, 32000);
        if (fuel > 0) {
            golem.setAttached(CopperGolemUpgrades.FUEL, fuel);
        }
    }
}
