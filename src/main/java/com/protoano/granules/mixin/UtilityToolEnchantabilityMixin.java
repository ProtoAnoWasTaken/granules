package com.protoano.granules.mixin;

import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ShearsItem.class, FlintAndSteelItem.class})
public abstract class UtilityToolEnchantabilityMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static Item.Properties granules$enchantableTools(Item.Properties properties) {
        return properties.enchantable(14);
    }
}
