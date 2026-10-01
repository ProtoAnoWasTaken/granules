package com.protoano.granules.client.mixin;

import com.protoano.granules.config.BalanceConfig;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BrewingStandScreen.class)
public abstract class BrewingBalanceScreenMixin {
    @ModifyConstant(method = "extractBackground", constant = @Constant(floatValue = 400.0F))
    private float brewingTime(float original) {
        return BalanceConfig.Setting.BREWING_TICKS.floatValue();
    }

    @ModifyConstant(method = "extractBackground", constant = @Constant(intValue = 20))
    private int fuelUses(int original) {
        return BalanceConfig.Setting.BREWING_FUEL_USES.intValue();
    }
}
