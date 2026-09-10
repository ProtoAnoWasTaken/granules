package com.puppy.granules.mixin;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.world.GlowSheepAccess;
import com.puppy.granules.rabbit.KillerRabbitAccess;
import com.puppy.granules.rabbit.RabbitContent;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGlowWoolLootMixin {
	@ModifyArg(
		method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Ljava/util/function/BiConsumer;)Z",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"
		),
		index = 0
	)
	private Consumer<ItemStack> granules$replaceGlowSheepWool(Consumer<ItemStack> output) {
		LivingEntity entity = (LivingEntity) (Object) this;
		Consumer<ItemStack> transformed = output;
		if (entity instanceof Animal && entity.getLastHurtByPlayer() instanceof Player player
			&& com.puppy.granules.rabbit.PalePeltEquipment.wearsHat(player)) {
			Consumer<ItemStack> next = transformed;
			transformed = stack -> next.accept(stack.copyWithCount(stack.getCount() + 1));
		}
		if ((Object) this instanceof KillerRabbitAccess rabbit && rabbit.granules$dropsPalePelt()) {
			Consumer<ItemStack> next = transformed;
			return stack -> {
				if (stack.is(net.minecraft.world.item.Items.RABBIT_HIDE)) {
					next.accept(new ItemStack(RabbitContent.PALE_PELT, stack.getCount()));
					return;
				}
				next.accept(stack);
			};
		}
		if (!((Object) this instanceof Sheep sheep)) {
			return transformed;
		}
		GlowSheepAccess glowSheep = (GlowSheepAccess) sheep;
		if (!glowSheep.granules$hasGlowWool()) {
			return transformed;
		}
		Consumer<ItemStack> next = transformed;
		return stack -> {
			if (stack.is(GranulesMod.getVanillaWoolBlock(sheep.getColor()).asItem())) {
				next.accept(new ItemStack(GranulesMod.getGlowWoolItem(sheep.getColor()), stack.getCount()));
				return;
			}
			next.accept(stack);
		};
	}

	@ModifyArg(
		method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"
		),
		index = 2
	)
	private Consumer<ItemStack> granules$replaceGlowSheepDeathWool(Consumer<ItemStack> output) {
		return granules$replaceGlowSheepWool(output);
	}
}
