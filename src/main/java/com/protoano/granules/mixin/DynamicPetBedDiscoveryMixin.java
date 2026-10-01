package com.protoano.granules.mixin;

import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryChangeTrigger.class)
public abstract class DynamicPetBedDiscoveryMixin {
    @Inject(method = "trigger", at = @At("TAIL"))
    private void discoverBeds(ServerPlayer player, Inventory inventory, ItemStack changed, CallbackInfo callback) {
        if (!changed.is(ItemTags.LOGS) && !changed.is(ItemTags.WOOL) && !changed.is(ItemTags.WOOL_CARPETS)
                && !changed.is(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        var recipes = player.level().getServer().getRecipeManager().getRecipes().stream()
            .filter(recipe -> recipe.id().identifier().getNamespace().equals("granules")
                && recipe.id().identifier().getPath().startsWith("dynamic_pet_bed/"))
            .filter(recipe -> recipe.value().placementInfo().ingredients().stream().anyMatch(ingredient -> ingredient.test(changed)))
            .toList();
        player.awardRecipes(recipes);
    }
}
