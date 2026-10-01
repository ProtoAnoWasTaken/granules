package com.protoano.granules.client;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;

public interface SplashReroller {
    SplashRenderer granules$reroll(Component previous);
}
