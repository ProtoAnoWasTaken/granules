package com.protoano.granules.client.mixin;

import com.protoano.granules.client.title.TitleRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class PlayerTitleRenderStateMixin implements TitleRenderState {
    @Unique
    private Component granules$title;

    @Override
    public Component granules$title() {
        return granules$title;
    }

    @Override
    public void granules$title(Component title) {
        granules$title = title;
    }
}
