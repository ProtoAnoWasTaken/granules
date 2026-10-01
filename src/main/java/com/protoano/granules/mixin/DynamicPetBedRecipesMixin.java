package com.protoano.granules.mixin;

import com.protoano.granules.pet.DynamicPetBeds;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public abstract class DynamicPetBedRecipesMixin {
    @Shadow
    private RecipeMap recipes;

    @Inject(method = "finalizeRecipeLoading", at = @At("HEAD"))
    private void addMaterialRecipes(FeatureFlagSet enabledFeatures, CallbackInfo callback) {
        recipes = DynamicPetBeds.addRecipes(recipes);
    }
}
