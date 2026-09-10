package com.puppy.granules.mixin;

import com.puppy.granules.enchantment.GranulesEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class EnchantedDurabilityMixin {
    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int applyCrumpling(int amount) {
        ItemStack self = (ItemStack) (Object) this;
        if (GranulesEnchantments.protectFiveDurability(self) && self.getMaxDamage() - self.getDamageValue() <= 5) {
            return 0;
        }
        if (amount > 0 && GranulesEnchantments.level(self, GranulesEnchantments.CURSE_OF_CRUMPLING) > 0 && java.util.concurrent.ThreadLocalRandom.current().nextFloat() < 0.25F) {
            return amount + 1;
        }
        return amount;
    }
}



