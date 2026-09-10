package com.puppy.granules.mixin;

import com.puppy.granules.world.NamedPetVariants;
import com.puppy.granules.advancement.GranulesAdvancements;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public abstract class NameTagItemMixin {
	@Inject(method = "interactLivingEntity", at = @At("RETURN"))
	private void granules$applyNamedPetVariant(
		ItemStack itemStack,
		Player player,
		LivingEntity target,
		InteractionHand hand,
		CallbackInfoReturnable<InteractionResult> callbackInfo
	) {
		if (!player.level().isClientSide() && callbackInfo.getReturnValue().consumesAction()) {
			NamedPetVariants.apply(target);
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				String name = target.getName().getString();
				if (name.equals("jeb_") && target.getType() == net.minecraft.world.entity.EntityTypes.SHEEP) {
					GranulesAdvancements.recordSpecialName(serverPlayer, "jeb");
				}
				if (name.equals("Dinnerbone") || name.equals("Grumm")) {
					GranulesAdvancements.recordSpecialName(serverPlayer, name.toLowerCase(java.util.Locale.ROOT));
				}
				if (name.equals("Toast") && target.getType() == net.minecraft.world.entity.EntityTypes.RABBIT) {
					GranulesAdvancements.recordSpecialName(serverPlayer, "toast");
				}
				if (name.equals("Johnny") && target.getType() == net.minecraft.world.entity.EntityTypes.VINDICATOR) {
					GranulesAdvancements.recordSpecialName(serverPlayer, "johnny");
				}
				if (name.equals("Dennis") && target.getType() == net.minecraft.world.entity.EntityTypes.WOLF) {
					GranulesAdvancements.recordSpecialName(serverPlayer, "dennis");
				}
			}
		}
	}
}
