package com.protoano.granules.client.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementWidget;
import com.protoano.granules.title.PlayerTitles;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(value = BetterAdvancementWidget.class, remap = false)
public abstract class BetterAdvancementWidgetMixin {
    @Shadow
    @Final
    private AdvancementNode advancementNode;

    @ModifyArg(method = "drawHover", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 4)
    private int granules$selectedTitleColor(int color) {
        var player = Minecraft.getInstance().player;
        var title = player == null ? null : player.getAttached(PlayerTitles.TITLE);
        if (color == -1 && title != null && title.advancement().equals(advancementNode.holder().id())) {
            return 0xFF55FF55;
        }
        return color;
    }
}
