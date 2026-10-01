package com.protoano.granules.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.protoano.granules.config.BalanceConfig;
import com.protoano.granules.golem.CopperGolemUpgrades;
import com.protoano.granules.golem.GolemContainerRouting;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemState;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class CopperGolemTransportMixin {
    @Shadow
    private TransportItemTarget target;
    @Shadow
    private TransportItemState state;
    @Shadow
    @Final
    private Predicate<BlockState> destinationBlockType;
    @Shadow
    protected abstract void clearMemoriesAfterMatchingTargetFound(PathfinderMob body);
    @Shadow
    private AABB getTargetSearchArea(PathfinderMob body) {
        throw new AssertionError();
    }
    @Shadow
    private int getHorizontalSearchDistance(PathfinderMob body) {
        throw new AssertionError();
    }
    @Shadow
    private boolean isContainerLocked(TransportItemTarget candidate) {
        throw new AssertionError();
    }
    @Shadow
    private boolean isPositionAlreadyVisited(Set<GlobalPos> visited, Set<GlobalPos> unreachable, TransportItemTarget candidate, Level level) {
        throw new AssertionError();
    }
    @Unique
    private double granules$workRemainder;

    @Inject(method = {"getHorizontalSearchDistance", "getVerticalSearchDistance"}, at = @At("RETURN"), cancellable = true)
    private void granules$extendSearch(PathfinderMob body, CallbackInfoReturnable<Integer> callback) {
        if (body instanceof CopperGolem golem && !body.isPassenger()) {
            callback.setReturnValue((int) Math.ceil(callback.getReturnValue() * CopperGolemUpgrades.rangeMultiplier(golem)));
        }
    }

    @WrapMethod(method = "onReachedTarget")
    private void granules$fasterWork(TransportItemTarget current, Level level, PathfinderMob body, Operation<Void> original) {
        original.call(current, level, body);
        if (body instanceof CopperGolem golem && CopperGolemUpgrades.boosted(golem)) {
            granules$workRemainder += BalanceConfig.Setting.COPPER_GOLEM_REDSTONE_SPEED.value() - 1;
            while (granules$workRemainder >= 1) {
                granules$workRemainder--;
                if (state != TransportItemState.INTERACTING || target != current) {
                    granules$workRemainder = 0;
                    break;
                }
                original.call(current, level, body);
            }
        } else {
            granules$workRemainder = 0;
        }
    }

    @Inject(method = "pickUpItems", at = @At("HEAD"), cancellable = true)
    private void granules$bundlePickup(PathfinderMob body, Container container, CallbackInfo callback) {
        if (body instanceof CopperGolem golem && CopperGolemUpgrades.get(golem).bundle()) {
            CopperGolemUpgrades.pickUp(golem, container);
            clearMemoriesAfterMatchingTargetFound(body);
            callback.cancel();
        }
    }

    @Inject(method = "putDownItem", at = @At("TAIL"))
    private void granules$nextBundleItem(PathfinderMob body, Container container, CallbackInfo callback) {
        if (body instanceof CopperGolem golem) {
            CopperGolemUpgrades.promoteReserve(golem);
        }
    }

    @Inject(method = "addItemsToContainer", at = @At("HEAD"), cancellable = true)
    private static void granules$safeInsertion(PathfinderMob body, Container container, CallbackInfoReturnable<ItemStack> callback) {
        if (body instanceof CopperGolem golem && (CopperGolemUpgrades.get(golem).bundle() || CopperGolemUpgrades.get(golem).priority())) {
            callback.setReturnValue(GolemContainerRouting.insert(container, body.getMainHandItem()));
        }
    }

    @Inject(method = "isWantedBlock", at = @At("HEAD"), cancellable = true)
    private void granules$markedContainers(PathfinderMob body, BlockState block, CallbackInfoReturnable<Boolean> callback) {
        if (body instanceof CopperGolem golem && CopperGolemUpgrades.get(golem).priority() && !body.getMainHandItem().isEmpty()) {
            callback.setReturnValue(block.hasBlockEntity() && !block.is(BlockTags.COPPER_CHESTS));
        }
    }

    @Inject(method = "getTransportTarget", at = @At("HEAD"), cancellable = true)
    private void granules$prioritizeFrames(ServerLevel level, PathfinderMob body, CallbackInfoReturnable<Optional<TransportItemTarget>> callback) {
        if (!(body instanceof CopperGolem golem) || !CopperGolemUpgrades.get(golem).priority() || body.getMainHandItem().isEmpty()) {
            return;
        }
        AABB area = getTargetSearchArea(body);
        var markers = GolemContainerRouting.markers(level, area);
        var visited = body.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
        var unreachable = body.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
        TransportItemTarget best = null;
        int bestRank = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (var chunkPos : ChunkPos.rangeClosed(ChunkPos.containing(body.blockPosition()), Math.floorDiv(getHorizontalSearchDistance(body), 16) + 1).toList()) {
            var chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk == null) {
                continue;
            }
            for (var blockEntity : chunk.getBlockEntities().values()) {
                var pos = blockEntity.getBlockPos();
                if (!area.contains(pos.getX(), pos.getY(), pos.getZ()) || blockEntity.getBlockState().is(BlockTags.COPPER_CHESTS)) {
                    continue;
                }
                var labels = GolemContainerRouting.labels(markers, pos, blockEntity.getBlockState());
                if (labels.isEmpty() && !destinationBlockType.test(blockEntity.getBlockState())) {
                    continue;
                }
                var candidate = TransportItemTarget.tryCreatePossibleTarget(blockEntity, level);
                if (candidate == null || isContainerLocked(candidate) || isPositionAlreadyVisited(visited, unreachable, candidate, level)
                    || !GolemContainerRouting.canInsert(candidate.container(), body.getMainHandItem())) {
                    continue;
                }
                int rank = GolemContainerRouting.rank(labels, body.getMainHandItem());
                if (rank != 0 && !GolemContainerRouting.normalMatch(candidate.container(), body.getMainHandItem())) {
                    continue;
                }
                double distance = pos.distToCenterSqr(body.position());
                if (rank < bestRank || rank == bestRank && distance < bestDistance) {
                    best = candidate;
                    bestRank = rank;
                    bestDistance = distance;
                }
            }
        }
        callback.setReturnValue(Optional.ofNullable(best));
    }

    @WrapOperation(method = "doReachedTargetInteraction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers;matchesLeavingItemsRequirement(Lnet/minecraft/world/entity/PathfinderMob;Lnet/minecraft/world/Container;)Z"))
    private boolean granules$acceptMarkedContents(PathfinderMob body, Container container, Operation<Boolean> original) {
        if (body instanceof CopperGolem golem && CopperGolemUpgrades.get(golem).priority() && target != null) {
            var markers = GolemContainerRouting.markers(body.level(), new AABB(target.pos()).inflate(1));
            if (GolemContainerRouting.rank(GolemContainerRouting.labels(markers, target.pos(), target.state()), body.getMainHandItem()) == 0) {
                return GolemContainerRouting.canInsert(container, body.getMainHandItem());
            }
        }
        return original.call(body, container);
    }

    @Inject(method = "isTargetBlocked", at = @At("HEAD"), cancellable = true)
    private void granules$nonChestAccess(Level level, TransportItemTarget current, CallbackInfoReturnable<Boolean> callback) {
        if (!(current.state().getBlock() instanceof ChestBlock)) {
            callback.setReturnValue(false);
        }
    }
}
