package com.protoano.granules.client.mixin;

import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AdvancementTab.class)
public interface TitleAdvancementTabAccessor {
    @Accessor("widgets")
    Map<AdvancementHolder, AdvancementWidget> granules$widgets();

    @Accessor("scrollX")
    double granules$scrollX();

    @Accessor("scrollY")
    double granules$scrollY();
}
