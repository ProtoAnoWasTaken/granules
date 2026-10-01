package com.protoano.granules.golem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

public final class GolemContainerRouting {
    private GolemContainerRouting() {
    }

    public static Map<BlockPos, List<ItemStack>> markers(Level level, AABB area) {
        var result = new HashMap<BlockPos, List<ItemStack>>();
        for (var frame : level.getEntitiesOfClass(ItemFrame.class, area.inflate(2))) {
            BlockPos support = frame.getPos().relative(frame.getDirection().getOpposite());
            result.computeIfAbsent(support, key -> new ArrayList<>()).add(frame.getItem());
        }
        return result;
    }

    public static List<ItemStack> labels(Map<BlockPos, List<ItemStack>> markers, BlockPos pos, BlockState state) {
        var result = new ArrayList<>(markers.getOrDefault(pos, List.of()));
        if (state.getBlock() instanceof ChestBlock && state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
            result.addAll(markers.getOrDefault(ChestBlock.getConnectedBlockPos(pos, state), List.of()));
        }
        return result;
    }

    public static int rank(List<ItemStack> labels, ItemStack carried) {
        if (labels.isEmpty()) {
            return 1;
        }
        return labels.stream().anyMatch(label -> !label.isEmpty() && ItemStack.isSameItem(label, carried)) ? 0 : 2;
    }

    public static boolean normalMatch(Container container, ItemStack carried) {
        if (container.isEmpty()) {
            return true;
        }
        for (var stack : container) {
            if (ItemStack.isSameItem(stack, carried)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canInsert(Container container, ItemStack carried) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            int limit = Math.min(container.getMaxStackSize(carried), carried.getMaxStackSize());
            if (container.canPlaceItem(slot, carried) && limit > 0
                && (stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, carried) && stack.getCount() < limit)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack insert(Container container, ItemStack carried) {
        for (int slot = 0; slot < container.getContainerSize() && !carried.isEmpty(); slot++) {
            if (!container.canPlaceItem(slot, carried)) {
                continue;
            }
            var existing = container.getItem(slot);
            int limit = Math.min(container.getMaxStackSize(carried), carried.getMaxStackSize());
            if (existing.isEmpty()) {
                container.setItem(slot, carried.split(limit));
            } else if (ItemStack.isSameItemSameComponents(existing, carried)) {
                int transferred = Math.min(carried.getCount(), Math.max(0, limit - existing.getCount()));
                existing.grow(transferred);
                carried.shrink(transferred);
                container.setItem(slot, existing);
            }
        }
        container.setChanged();
        return carried.isEmpty() ? ItemStack.EMPTY : carried;
    }
}
