package com.protoano.granules.tea;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public record TeaRecipe(ShapelessRecipe delegate) implements CraftingRecipe {
    public static final MapCodec<TeaRecipe> MAP_CODEC = ShapelessRecipe.MAP_CODEC.xmap(TeaRecipe::new, TeaRecipe::delegate);
    public static final StreamCodec<RegistryFriendlyByteBuf, TeaRecipe> STREAM_CODEC = ShapelessRecipe.STREAM_CODEC.map(TeaRecipe::new, TeaRecipe::delegate);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!delegate.matches(input, level)) {
            return false;
        }
        for (int slot = 0; slot < input.size(); slot++) {
            var stack = input.getItem(slot);
            if (stack.is(Items.POTION)) {
                var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                if (!potion.is(Potions.WATER) || !potion.customEffects().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return delegate.assemble(input);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<TeaRecipe> getSerializer() {
        return TeaContent.RECIPE_SERIALIZER;
    }

    @Override
    public CraftingBookCategory category() {
        return delegate.category();
    }

    @Override
    public String group() {
        return delegate.group();
    }

    @Override
    public boolean showNotification() {
        return delegate.showNotification();
    }

    @Override
    public PlacementInfo placementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return delegate.display().stream().map(display -> {
            var shapeless = (ShapelessCraftingRecipeDisplay) display;
            List<SlotDisplay> ingredients = shapeless.ingredients().stream().map(TeaRecipe::waterDisplay).toList();
            return (RecipeDisplay) new ShapelessCraftingRecipeDisplay(ingredients, shapeless.result(), shapeless.craftingStation());
        }).toList();
    }

    private static SlotDisplay waterDisplay(SlotDisplay display) {
        if (display instanceof SlotDisplay.ItemSlotDisplay item && item.item().value() == Items.POTION) {
            return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(PotionContents.createItemStack(Items.POTION, Potions.WATER)));
        }
        return display;
    }
}
