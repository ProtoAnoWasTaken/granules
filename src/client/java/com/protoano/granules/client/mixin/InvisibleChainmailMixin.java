package com.protoano.granules.client.mixin;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class InvisibleChainmailMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void hideInvisibleChainmail(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo callback) {
        if (!(avatar instanceof Player) || !avatar.hasEffect(MobEffects.INVISIBILITY)) {
            return;
        }
        if (state.headEquipment.is(Items.CHAINMAIL_HELMET)) {
            state.headEquipment = ItemStack.EMPTY;
        }
        if (state.chestEquipment.is(Items.CHAINMAIL_CHESTPLATE)) {
            state.chestEquipment = ItemStack.EMPTY;
        }
        if (state.legsEquipment.is(Items.CHAINMAIL_LEGGINGS)) {
            state.legsEquipment = ItemStack.EMPTY;
        }
        if (state.feetEquipment.is(Items.CHAINMAIL_BOOTS)) {
            state.feetEquipment = ItemStack.EMPTY;
        }
    }
}
