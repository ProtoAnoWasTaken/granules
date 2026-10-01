package com.protoano.granules.item;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PaintingLocks {
    public static final String LOCKED = "granules:locked_painting";

    private PaintingLocks() {
    }

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof Painting painting) || !player.getItemInHand(hand).is(UtilityItems.IRON_LOCK)
                || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            if (!level.mayInteract(player, painting.blockPosition()) || !player.getAbilities().mayBuild) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                if (painting.entityTags().contains(LOCKED)) {
                    painting.removeTag(LOCKED);
                } else if (!painting.addTag(LOCKED)) {
                    return InteractionResult.FAIL;
                }
                painting.playSound(painting.entityTags().contains(LOCKED)
                    ? com.protoano.granules.sound.GranulesSounds.LOCK : com.protoano.granules.sound.GranulesSounds.UNLOCK, 1.0F, 1.0F);
                player.getItemInHand(hand).hurtAndBreak(1, player, hand);
                com.protoano.granules.advancement.GranulesAdvancements.recordLock(player, painting.entityTags().contains(LOCKED));
            }
            return InteractionResult.SUCCESS;
        });
    }

    public static ItemStack fixedItem(Painting painting) {
        ItemStack stack = new ItemStack(Items.PAINTING);
        stack.set(DataComponents.PAINTING_VARIANT, painting.getVariant());
        return stack;
    }
}
