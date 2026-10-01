package com.protoano.granules.client;

import net.minecraft.network.chat.Component;

public interface SplashHitbox {
    boolean granules$contains(double mouseX, double mouseY);

    Component granules$text();
}
