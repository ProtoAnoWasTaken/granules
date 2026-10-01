package com.protoano.granules.client.mixin;

import com.protoano.granules.client.HearthearthRenderStateAccess;
import com.protoano.granules.enchantment.HearthearthCampfire;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CampfireRenderState.class)
public abstract class HearthearthRenderStateMixin implements HearthearthRenderStateAccess {
    @Unique
    private final ItemStackRenderState[] granules$swords = createSwords();

    @Unique
    private static ItemStackRenderState[] createSwords() {
        ItemStackRenderState[] swords = new ItemStackRenderState[HearthearthCampfire.CAPACITY];
        for (int index = 0; index < swords.length; index++) {
            swords[index] = new ItemStackRenderState();
        }
        return swords;
    }

    @Override
    public ItemStackRenderState[] granules$plantedSwords() {
        return granules$swords;
    }
}
