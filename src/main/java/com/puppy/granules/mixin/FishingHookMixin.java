package com.puppy.granules.mixin;

import com.puppy.granules.world.OldWorldFishing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	@Redirect(
		method = "retrieve",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
		)
	)
	private ObjectArrayList<ItemStack> modifyFishingLoot(LootTable lootTable, LootParams parameters, ItemStack rod) {
		ObjectArrayList<ItemStack> caughtItems = lootTable.getRandomItems(parameters);
		FishingHook fishingHook = (FishingHook)(Object)this;
		if (!(fishingHook.level() instanceof ServerLevel serverLevel)) {
			return caughtItems;
		}
		List<ItemStack> modifiedItems = OldWorldFishing.modifyFishingLoot(serverLevel, fishingHook.position(), rod, caughtItems, parameters);
		return new ObjectArrayList<>(modifiedItems);
	}
}
