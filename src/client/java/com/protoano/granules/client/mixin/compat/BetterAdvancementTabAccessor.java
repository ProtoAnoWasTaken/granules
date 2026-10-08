package com.protoano.granules.client.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementWidget;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = BetterAdvancementTab.class, remap = false)
public interface BetterAdvancementTabAccessor {
    @Accessor("widgets")
    Map<AdvancementHolder, BetterAdvancementWidget> granules$widgets();

    @Accessor("scrollX")
    int granules$scrollX();

    @Accessor("scrollY")
    int granules$scrollY();
}
