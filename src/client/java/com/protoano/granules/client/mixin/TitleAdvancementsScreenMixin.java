package com.protoano.granules.client.mixin;

import com.protoano.granules.title.PlayerTitles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementsScreen.class)
public class TitleAdvancementsScreenMixin {
    @Shadow
    private AdvancementTab selectedTab;
    @Shadow
    private int leftPos;
    @Shadow
    private int topPos;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void granules$selectTitle(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callback) {
        if (event.button() != 0 || selectedTab == null || !ClientPlayNetworking.canSend(PlayerTitles.Select.TYPE)) {
            return;
        }
        int x = (int) event.x() - leftPos - 9;
        int y = (int) event.y() - topPos - 18;
        if (x < 0 || x >= 234 || y < 0 || y >= 113) {
            return;
        }
        var tab = (TitleAdvancementTabAccessor) selectedTab;
        for (var entry : tab.granules$widgets().entrySet()) {
            var widget = entry.getValue();
            var progress = ((TitleAdvancementWidgetAccessor) widget).granules$progress();
            if (progress != null && progress.isDone() && widget.isMouseOver(Mth.floor(tab.granules$scrollX()), Mth.floor(tab.granules$scrollY()), x, y)) {
                ClientPlayNetworking.send(new PlayerTitles.Select(entry.getKey().id()));
                callback.setReturnValue(true);
                return;
            }
        }
    }
}
