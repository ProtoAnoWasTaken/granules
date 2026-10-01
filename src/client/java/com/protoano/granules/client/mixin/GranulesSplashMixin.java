package com.protoano.granules.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.protoano.granules.client.SplashReroller;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashManager.class)
public abstract class GranulesSplashMixin implements SplashReroller {
    @Shadow
    private List<Component> splashes;

    @Unique
    private static final List<String> granules$additionalSplashes = List.of(
        "What does the cow say?",
        "At the crossroads...",
        "No one chooses to jump for the beef.",
        "So blocky, so cool!",
        "Hey guys, <username> here!",
        "Rocket Launcher!",
        "Also try Fractal Block World!",
        "Also try Lucid Blocks!",
        "Also try Balatro!",
        "Don't climb the tower.",
        "Seaweed is good!",
        "We're gonna go on a journey...",
        "...that will probably end in us dying horribly.",
        "It needs to be something funny, and sort of light-hearted.",
        "I'm Proto, you're Ano!",
        "Me? Gongaga.",
        "They put bugs in him!",
        "Your personal helper friend!",
        "E!!!!!!!",
        "Welcome to <username>'s Lovely World!",
        "That was when I ruled the land...",
        "Chicken jockey!",
        "Nothing built can last forever.",
        "The most fun you can have in an app!"
    );

    @Inject(method = "apply(Ljava/util/List;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void granules$addSplashes(List<Component> preparations, ResourceManager manager, ProfilerFiller profiler, CallbackInfo callback) {
        List<Component> combined = new ArrayList<>(this.splashes);
        for (String text : granules$additionalSplashes) {
            combined.add(Component.literal(text).withColor(0xFFFF00));
        }
        this.splashes = List.copyOf(combined);
    }

    @ModifyExpressionValue(method = "getSplash", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    private Object granules$personalizeSplash(Object selected) {
        if (!(selected instanceof Component splash) || !granules$additionalSplashes.contains(splash.getString())
            || !splash.getString().contains("<username>")) {
            return selected;
        }
        Minecraft minecraft = Minecraft.getInstance();
        List<String> names = new ArrayList<>();
        names.add(minecraft.getUser().getName());
        var social = minecraft.getPlayerSocialManager();
        for (var friend : social.getFriends()) {
            if (friend.name() != null && !friend.name().isBlank() && !social.isBlocked(friend.id())
                && !social.isHidden(friend.id()) && !names.contains(friend.name())) {
                names.add(friend.name());
            }
        }
        String name = names.get(RandomSource.create().nextInt(names.size()));
        return Component.literal(splash.getString().replace("<username>", name)).setStyle(splash.getStyle());
    }

    @Override
    public SplashRenderer granules$reroll(Component previous) {
        if (this.splashes.isEmpty()) {
            return new SplashRenderer(previous);
        }
        int start = RandomSource.create().nextInt(this.splashes.size());
        for (int offset = 0; offset < this.splashes.size(); offset++) {
            Component candidate = (Component)this.granules$personalizeSplash(this.splashes.get((start + offset) % this.splashes.size()));
            if (!candidate.getString().equals(previous.getString())) {
                return new SplashRenderer(candidate);
            }
        }
        return new SplashRenderer(previous);
    }
}
