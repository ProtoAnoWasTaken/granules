package com.protoano.granules.client.mixin;

import com.protoano.granules.client.SplashHitbox;
import com.protoano.granules.client.SplashReroller;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenSplashMixin extends Screen {
    @Shadow
    private SplashRenderer splash;

    protected TitleScreenSplashMixin(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void granules$rerollClickedSplash(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callback) {
        if (event.button() != 0 || this.splash == null || this.minecraft.options.hideSplashTexts().get()) {
            return;
        }
        SplashHitbox hitbox = (SplashHitbox)this.splash;
        if (!hitbox.granules$contains(event.x(), event.y())) {
            return;
        }
        this.splash = ((SplashReroller)this.minecraft.gui.splashManager()).granules$reroll(hitbox.granules$text());
        AbstractWidget.playButtonClickSound(this.minecraft.getSoundManager());
        callback.setReturnValue(true);
    }
}
