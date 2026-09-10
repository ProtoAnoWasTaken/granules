package com.puppy.granules.fletching;

import com.puppy.granules.entity.FletchersArrowEntity;
import com.puppy.granules.GranulesMod;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class FletchersArrowItem extends ArrowItem {
	public FletchersArrowItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(
		ItemStack stack,
		Item.TooltipContext context,
		TooltipDisplay display,
		Consumer<Component> tooltip,
		TooltipFlag flag
	) {
		ArrowParts parts = partsOf(stack);
		tooltip.accept(Component.translatable(
			"granules.fletchers_arrow.tooltip.head",
			this.headName(parts)
		).withStyle(ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.translatable(
			"granules.fletchers_arrow.tooltip.shaft",
			this.shaftName(parts)
		).withStyle(ChatFormatting.DARK_GRAY));
		tooltip.accept(Component.translatable(
			"granules.fletchers_arrow.tooltip.fletch",
			this.fletchName(parts)
		).withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity owner, @Nullable ItemStack firedFromWeapon) {
		return new FletchersArrowEntity(level, owner, itemStack.copyWithCount(1), firedFromWeapon, partsOf(itemStack));
	}

	@Override
	public Projectile asProjectile(Level level, net.minecraft.core.Position position, ItemStack itemStack, net.minecraft.core.Direction direction) {
		FletchersArrowEntity arrow = new FletchersArrowEntity(
			level,
			position.x(),
			position.y(),
			position.z(),
			itemStack.copyWithCount(1),
			null,
			partsOf(itemStack)
		);
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		return arrow;
	}

	public static ArrowParts partsOf(ItemStack stack) {
		CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
		if (modelData != null) {
			String suffix = modelData.getString(0);
			if (suffix != null) {
				return ArrowParts.fromIdentifierSuffix(suffix).orElseGet(FletchersArrowItem::basicParts);
			}
		}
		return basicParts();
	}

	public static ItemStack createStack(ArrowParts parts, int count) {
		ItemStack stack = new ItemStack(GranulesMod.FLETCHERS_ARROW, count);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(parts.identifierSuffix()), List.of()));
		return stack;
	}

	private Component headName(ArrowParts parts) {
		if (parts.head() == ArrowParts.Head.FLINT) {
			return Component.translatable("granules.fletchers_arrow.standard");
		}
		return Component.translatable("granules.fletchers_arrow.head." + parts.head().name().toLowerCase(java.util.Locale.ROOT));
	}

	private Component shaftName(ArrowParts parts) {
		if (parts.shaft() == ArrowParts.Shaft.STICK) {
			return Component.translatable("granules.fletchers_arrow.standard");
		}
		return Component.translatable("granules.fletchers_arrow.shaft." + parts.shaft().name().toLowerCase(java.util.Locale.ROOT));
	}

	private Component fletchName(ArrowParts parts) {
		if (parts.fletch() == ArrowParts.Fletch.FEATHER) {
			return Component.translatable("granules.fletchers_arrow.standard");
		}
		return Component.translatable("granules.fletchers_arrow.fletch." + parts.fletch().name().toLowerCase(java.util.Locale.ROOT));
	}

	private static ArrowParts basicParts() {
		return new ArrowParts(ArrowParts.Shaft.STICK, ArrowParts.Fletch.FEATHER, ArrowParts.Head.FLINT);
	}
}
