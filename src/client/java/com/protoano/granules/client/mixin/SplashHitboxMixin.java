package com.protoano.granules.client.mixin;

import com.protoano.granules.client.SplashHitbox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashRenderer.class)
public abstract class SplashHitboxMixin implements SplashHitbox {
    @Shadow
    @Final
    private Component splash;

    @Unique
    private Matrix3x2f granules$inversePose;

    @Unique
    private int granules$textWidth;

    @Unique
    private int granules$textHeight;

    @Unique
    private boolean granules$visible;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void granules$measureSplash(GuiGraphicsExtractor graphics, int screenWidth, Font font, float alpha, CallbackInfo callback) {
        this.granules$textWidth = font.width(this.splash);
        this.granules$textHeight = font.lineHeight;
        this.granules$visible = alpha > 0.0F;
    }

    @ModifyArg(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector$Parameters;withPose(Lorg/joml/Matrix3x2fc;)Lnet/minecraft/client/gui/ActiveTextCollector$Parameters;"),
        index = 0
    )
    private Matrix3x2fc granules$captureSplashPose(Matrix3x2fc pose) {
        this.granules$inversePose = new Matrix3x2f(pose).invert();
        return pose;
    }

    @Override
    public boolean granules$contains(double mouseX, double mouseY) {
        if (!this.granules$visible || this.granules$inversePose == null || this.granules$textWidth == 0) {
            return false;
        }
        Vector2f local = this.granules$inversePose.transformPosition((float)mouseX, (float)mouseY, new Vector2f());
        int left = -this.granules$textWidth / 2;
        return local.x >= left - 2 && local.x <= left + this.granules$textWidth + 2
            && local.y >= -10 && local.y <= this.granules$textHeight - 6;
    }

    @Override
    public Component granules$text() {
        return this.splash;
    }
}
