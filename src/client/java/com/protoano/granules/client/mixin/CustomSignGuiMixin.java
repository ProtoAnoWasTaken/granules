package com.protoano.granules.client.mixin;

import com.protoano.granules.aspen.AspenContent;
import com.protoano.granules.chorus.ChorusContent;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SignEditScreen.class, HangingSignEditScreen.class})
public abstract class CustomSignGuiMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/Identifier;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;"))
    private Identifier granules$customSignTexture(String original, SignBlockEntity sign, boolean front, boolean filtered) {
        var block = sign.getBlockState().getBlock();
        String wood;
        if (block == AspenContent.SIGN || block == AspenContent.WALL_SIGN
            || block == AspenContent.HANGING_SIGN || block == AspenContent.WALL_HANGING_SIGN) {
            wood = "aspen";
        } else if (block == ChorusContent.SIGN || block == ChorusContent.WALL_SIGN
            || block == ChorusContent.HANGING_SIGN || block == ChorusContent.WALL_HANGING_SIGN) {
            wood = "chorus";
        } else {
            return Identifier.withDefaultNamespace(original);
        }
        String folder = (Object) this instanceof HangingSignEditScreen ? "hanging_signs" : "signs";
        return Identifier.fromNamespaceAndPath("granules", "textures/gui/" + folder + "/" + wood + ".png");
    }
}
