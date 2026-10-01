package com.protoano.granules.client.mixin;

import com.protoano.granules.title.PlayerTitles;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AdvancementWidget.class)
public class TitleAdvancementWidgetMixin {
    @Shadow
    @Final
    private AdvancementNode advancementNode;

    @ModifyArg(method = "extractHover", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;extractMultilineText(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;III)V"), index = 4)
    private int granules$selectedTitleColor(int color) {
        var player = Minecraft.getInstance().player;
        var title = player == null ? null : player.getAttached(PlayerTitles.TITLE);
        return color == -1 && title != null && title.advancement().equals(advancementNode.holder().id()) ? 0xFF55FF55 : color;
    }
}
