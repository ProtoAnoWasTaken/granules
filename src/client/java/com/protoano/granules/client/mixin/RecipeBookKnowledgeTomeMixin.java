package com.protoano.granules.client.mixin;

import com.protoano.granules.item.EnchantedTomeItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.util.context.ContextMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookKnowledgeTomeMixin {
	@Unique
	private static final Identifier GRANULES_UNLOCKED_TEXTURE = Identifier.fromNamespaceAndPath("granules", "textures/gui/recipe_book_unlocked.png");
	@Shadow
	@Final
	private StackedItemContents stackedContents;
	@Shadow
	protected abstract void selectMatchingRecipes(RecipeCollection collection, StackedItemContents contents);
	@Invoker("isFiltering")
	protected abstract boolean granules$isFiltering();
	@Invoker("updateCollections")
	protected abstract void granules$updateCollections(boolean resetPage, boolean filtering);
	@Shadow
	private int width;
	@Shadow
	private int height;
	@Shadow
	private int xOffset;
	@Shadow
	private boolean visible;
	@Shadow
	private Minecraft minecraft;
	@Unique
	private ItemStack granules$input = ItemStack.EMPTY;

	@ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"), index = 1)
	private Identifier granules$replaceBackground(Identifier original) {
		return this.granules$enabled() ? GRANULES_UNLOCKED_TEXTURE : original;
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void renderInput(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo callbackInfo) {
		if (!this.granules$enabled()) {
			return;
		}
		int x = this.granules$inputX();
		int y = this.granules$inputY();
		graphics.blit(RenderPipelines.GUI_TEXTURED, GRANULES_UNLOCKED_TEXTURE, x, y, 8.0F, 167.0F, 22, 22, 256, 256);
		graphics.item(this.granules$input, x + 3, y + 2);
		if (this.granules$isOverInput(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(Component.translatable("item.granules.enchanted_tome.recipe_book_ingredient"), mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void setInput(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (this.granules$enabled() && this.granules$isOverInput(event.x(), event.y())) {
			if (event.button() != 0 && event.button() != 1) {
				callbackInfo.setReturnValue(true);
				return;
			}
			ItemStack carried = this.minecraft.player.containerMenu.getCarried();
			this.granules$input = event.button() == 1 || carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
			this.granules$updateCollections(true, this.granules$isFiltering());
			callbackInfo.setReturnValue(true);
		}
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void granules$keepInputInside(double mouseX, double mouseY, int left, int top, int width, int height, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (this.granules$enabled() && this.granules$isOverInput(mouseX, mouseY)) {
			callbackInfo.setReturnValue(false);
		}
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void granules$clearUnavailableInput(CallbackInfo callbackInfo) {
		if (this.visible && !this.granules$input.isEmpty() && !this.granules$enabled()) {
			this.granules$input = ItemStack.EMPTY;
			this.granules$updateCollections(true, this.granules$isFiltering());
		}
	}

	@Redirect(method = "updateCollections", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookPage;updateCollections(Ljava/util/List;ZZ)V"))
	private void filterInputRecipes(RecipeBookPage page, List<RecipeCollection> collections, boolean resetPage, boolean filtering) {
		if (!this.granules$enabled() || this.granules$input.isEmpty()) {
			page.updateCollections(collections, resetPage, filtering);
			return;
		}
		ContextMap displayContext = SlotDisplayContext.fromLevel(this.minecraft.level);
		List<RecipeCollection> matches = collections.stream()
			.map(collection -> {
				RecipeCollection match = new RecipeCollection(collection.getRecipes().stream()
					.filter(entry -> entry.craftingRequirements().stream()
						.flatMap(List::stream)
						.anyMatch(ingredient -> ingredient.test(this.granules$input))
						|| entry.display().result().resolveForStacks(displayContext).stream()
							.anyMatch(result -> result.is(this.granules$input.getItem())))
					.toList());
				this.selectMatchingRecipes(match, this.stackedContents);
				return match;
			})
			.filter(RecipeCollection::hasAnySelected)
			.filter(collection -> !filtering || collection.hasCraftable())
			.toList();
		page.updateCollections(matches, resetPage, filtering);
	}

	@Unique
	private int granules$inputX() {
		return (this.width - 147) / 2 - this.xOffset + 7;
	}

	@Unique
	private int granules$inputY() {
		return (this.height - 166) / 2 + 166;
	}

	@Unique
	private boolean granules$isOverInput(double mouseX, double mouseY) {
		int x = this.granules$inputX();
		int y = this.granules$inputY();
		return mouseX >= x && mouseX < x + 22 && mouseY >= y && mouseY < y + 22;
	}

	@Unique
	private boolean granules$enabled() {
		if (!this.visible || this.minecraft.player == null) {
			return false;
		}
		return EnchantedTomeItem.hasUnlockedKnowledge(this.minecraft.player);
	}
}
