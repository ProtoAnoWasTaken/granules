package com.protoano.granules.enchantment;

import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

public final class Hearthearth {
    public static final TagKey<Item> SWORDS = TagKey.create(Registries.ITEM,
        Identifier.fromNamespaceAndPath("granules", "enchantable/hearthearth_swords"));

    private Hearthearth() {
    }

    public static boolean isSword(ItemStack stack) {
        return stack.is(SWORDS) && stack.isDamageableItem()
            && GranulesEnchantments.level(stack, GranulesEnchantments.HEARTHEARTH) > 0;
    }

    public static boolean isExhausted(ItemStack stack) {
        return isSword(stack) && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            for (var blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof CampfireBlockEntity campfire) {
                    for (var entry : HearthearthCampfire.get(campfire).swords()) {
                        HearthearthOwnership.get(level).record(campfire, entry.owner());
                    }
                }
            }
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (isExhausted(player.getItemInHand(hand))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator() || !player.mayBuild()
                    || !(level.getBlockEntity(hit.getBlockPos()) instanceof CampfireBlockEntity campfire)) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            var planted = HearthearthCampfire.get(campfire);
            if (isSword(stack)) {
                if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                    if (planted.plant(campfire, player, stack)) {
                        bindRespawn(serverPlayer, serverLevel, campfire.getBlockPos());
                    } else {
                        serverPlayer.sendSystemMessage(Component.translatable(HearthearthOwnership.get(serverLevel).hasSword(player)
                            ? "message.granules.hearthearth.already_planted" : "message.granules.hearthearth.full"), true);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (hand == InteractionHand.MAIN_HAND && stack.isEmpty() && planted.ownsSword(player.getUUID())) {
                if (level instanceof ServerLevel) {
                    planted.retrieve(campfire, player);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    public static void bindRespawn(ServerPlayer player, ServerLevel level, BlockPos pos) {
        boolean lit = CampfireBlock.isLitCampfire(level.getBlockState(pos));
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
            LevelData.RespawnData.of(level.dimension(), pos, player.getYRot(), 0.0F), false), lit);
        if (!lit) {
            player.sendSystemMessage(Component.translatable("message.granules.hearthearth.unlit"));
        }
    }

    public static Optional<Vec3> respawnPosition(ServerLevel level, BlockPos pos, java.util.UUID owner) {
        level.getChunkAt(pos);
        if (!CampfireBlock.isLitCampfire(level.getBlockState(pos))
                || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire)
                || !HearthearthCampfire.get(campfire).ownsSword(owner)) {
            return Optional.empty();
        }
        for (int height : new int[] {0, 1, -1}) {
            for (int[] offset : new int[][] {{0, 2}, {2, 0}, {0, -2}, {-2, 0}, {1, 2}, {2, 1},
                    {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
                BlockPos candidate = pos.offset(offset[0], height, offset[1]);
                if (!level.getWorldBorder().isWithinBounds(candidate)
                        || !level.getFluidState(candidate).isEmpty()
                        || !level.getFluidState(candidate.above()).isEmpty()
                        || !level.getFluidState(candidate.below()).isEmpty()) {
                    continue;
                }
                Vec3 location = DismountHelper.findSafeDismountLocation(EntityTypes.PLAYER, level, candidate, true);
                if (location != null) {
                    return Optional.of(location);
                }
            }
        }
        return Optional.empty();
    }
}
