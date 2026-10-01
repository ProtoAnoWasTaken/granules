package com.protoano.granules.client.golem;

import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.resources.Identifier;

public final class GolemEyeTextures {
    private GolemEyeTextures() {
    }

    public static Identifier upgraded(CopperGolemRenderState state) {
        int eyes = ((GolemUpgradeRenderState) state).granules$eyes();
        if (eyes == 0) {
            return null;
        }
        String suffix = eyes == 2 ? "_u2" : switch (state.weathering) {
            case UNAFFECTED -> "_u1";
            case EXPOSED -> "_exposed_u1";
            case WEATHERED -> "_weathered_u1";
            case OXIDIZED -> "_oxidized_u1";
        };
        return Identifier.fromNamespaceAndPath("granules", "textures/entity/copper_golem/copper_golem_eyes" + suffix + ".png");
    }
}
