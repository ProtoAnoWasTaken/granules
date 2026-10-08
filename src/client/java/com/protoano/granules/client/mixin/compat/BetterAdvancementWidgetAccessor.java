package com.protoano.granules.client.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementWidget;
import net.minecraft.advancements.AdvancementProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = BetterAdvancementWidget.class, remap = false)
public interface BetterAdvancementWidgetAccessor {
    @Accessor("advancementProgress")
    AdvancementProgress granules$progress();
}
