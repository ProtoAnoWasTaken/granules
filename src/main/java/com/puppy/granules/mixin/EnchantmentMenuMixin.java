package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Unique
	private static final ThreadLocal<CandleContext> granules$candleContext = new ThreadLocal<>();

	@Inject(method = "lambda$slotsChanged$0", at = @At("HEAD"))
	private void captureCandleContext(ItemStack stack, Level level, BlockPos tablePos, CallbackInfo callbackInfo) {
		granules$candleContext.set(new CandleContext(level, tablePos));
	}

	@ModifyArg(
		method = "lambda$slotsChanged$0",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"),
		index = 2
	)
	private int applyCandleEnchantmentPower(int bookshelfPower) {
		CandleContext context = granules$candleContext.get();
		if (context == null) {
			return bookshelfPower;
		}
		int candleQuarters = 0;
		int philosopherBlocks = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			BlockState state = context.level().getBlockState(context.tablePos().offset(offset));
			if (state.getBlock() instanceof CandleBlock) {
				if (state.getValue(CandleBlock.LIT)) {
					candleQuarters += state.getValue(CandleBlock.CANDLES);
				}
			}
			if (state.is(GranulesMod.PHILOSOPHER)) {
				philosopherBlocks++;
			}
		}
		return bookshelfPower + candleQuarters / 4 + philosopherBlocks * 4;
	}

	@Redirect(
		method = "slotsChanged",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEnchantable()Z")
	)
	private boolean allowExistingEnchantments(ItemStack stack) {
		return stack.isEnchantable() || isEnchantedTable();
	}

	@Redirect(
		method = "getEnchantmentList",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Ljava/util/List;")
	)
	private List<EnchantmentInstance> selectEnchantedTableResults(
		net.minecraft.util.RandomSource random,
		ItemStack stack,
		int level,
		java.util.stream.Stream<Holder<Enchantment>> enchantments
	) {
		if (!isEnchantedTable()) {
			return EnchantmentHelper.selectEnchantment(random, stack, level, enchantments);
		}
		ItemEnchantments existing = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		ItemStack unenchantedCopy = stack.copy();
		EnchantmentHelper.setEnchantments(unenchantedCopy, ItemEnchantments.EMPTY);
		List<EnchantmentInstance> selected = EnchantmentHelper.selectEnchantment(random, unenchantedCopy, level, enchantments);
		List<EnchantmentInstance> compatible = new ArrayList<>();
		for (EnchantmentInstance candidate : selected) {
			boolean valid = true;
			for (Holder<Enchantment> existingEnchantment : existing.keySet()) {
				if (!existingEnchantment.equals(candidate.enchantment()) && !Enchantment.areCompatible(existingEnchantment, candidate.enchantment())) {
					valid = false;
					break;
				}
			}
			if (valid) {
				compatible.add(candidate);
			}
		}
		return compatible;
	}

	@Redirect(
		method = "lambda$clickMenuButton$0",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;enchant(Lnet/minecraft/core/Holder;I)V")
	)
	private void addEnchantmentLevels(ItemStack stack, Holder<Enchantment> enchantment, int level) {
		if (!isEnchantedTable()) {
			stack.enchant(enchantment, level);
			return;
		}
		EnchantmentHelper.updateEnchantments(stack, mutable -> {
			int currentLevel = mutable.getLevel(enchantment);
			int newLevel = Math.min(enchantment.value().getMaxLevel(), currentLevel + level);
			mutable.set(enchantment, newLevel);
		});
	}

	@Unique
	private boolean isEnchantedTable() {
		boolean[] result = {false};
		access.execute((level, pos) -> result[0] = level.getBlockState(pos).is(GranulesMod.ENCHANTED_ENCHANTING_TABLE));
		return result[0];
	}

	@Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
	private void remainValidAtEnchantedTable(net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (isEnchantedTable()) {
			callbackInfo.setReturnValue(true);
		}
	}

	@Unique
	private record CandleContext(Level level, BlockPos tablePos) {
	}
}
