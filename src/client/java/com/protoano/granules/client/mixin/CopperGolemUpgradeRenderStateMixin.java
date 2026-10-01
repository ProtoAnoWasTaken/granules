package com.protoano.granules.client.mixin;

import com.protoano.granules.client.golem.GolemUpgradeRenderState;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CopperGolemRenderState.class)
public class CopperGolemUpgradeRenderStateMixin implements GolemUpgradeRenderState {
    @Unique
    private int granules$eyes;

    @Override
    public int granules$eyes() {
        return granules$eyes;
    }

    @Override
    public void granules$eyes(int eyes) {
        granules$eyes = eyes;
    }
}
