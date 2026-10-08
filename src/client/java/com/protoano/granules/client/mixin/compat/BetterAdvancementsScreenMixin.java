package com.protoano.granules.client.mixin.compat;

import betteradvancements.common.gui.BetterAdvancementTab;
import betteradvancements.common.gui.BetterAdvancementsScreen;
import com.protoano.granules.title.PlayerTitles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = BetterAdvancementsScreen.class, remap = false)
public abstract class BetterAdvancementsScreenMixin extends Screen {
    @Shadow
    private BetterAdvancementTab selectedTab;
    @Shadow
    protected int internalWidth;
    @Shadow
    protected int internalHeight;
    @Shadow
    public static float zoom;

    protected BetterAdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void granules$selectTitle(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callback) {
        if (event.button() != 0 || event.modifiers() != 0 || selectedTab == null
                || !ClientPlayNetworking.canSend(PlayerTitles.Select.TYPE)) {
            return;
        }
        double x = event.x() - 39 - (width - internalWidth) / 2;
        double y = event.y() - 58 - (height - internalHeight) / 2;
        if (x < 0 || y < 0 || x >= internalWidth - 78 || y >= internalHeight - 97) {
            return;
        }
        var tab = (BetterAdvancementTabAccessor) selectedTab;
        for (var entry : tab.granules$widgets().entrySet()) {
            var widget = entry.getValue();
            var progress = ((BetterAdvancementWidgetAccessor) widget).granules$progress();
            if (progress != null && progress.isDone()
                    && widget.isMouseOver(tab.granules$scrollX(), tab.granules$scrollY(), x, y, zoom)) {
                ClientPlayNetworking.send(new PlayerTitles.Select(entry.getKey().id()));
                callback.setReturnValue(true);
                return;
            }
        }
    }
}
