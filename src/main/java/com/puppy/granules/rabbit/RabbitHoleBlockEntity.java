package com.puppy.granules.rabbit;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.puppy.granules.advancement.GranulesAdvancements;

public final class RabbitHoleBlockEntity extends BlockEntity {
    private final Set<UUID> knownPlayers = new HashSet<>();
    private int charges;
    private long lastSpawnNight = Long.MIN_VALUE;
    private long linkedPosition = Long.MIN_VALUE;

    public RabbitHoleBlockEntity(BlockPos pos, BlockState state) {
        super(RabbitHoleContent.BLOCK_ENTITY, pos, state);
    }

    public void prime(ServerLevel level) {
        rememberPlayers(level);
        charges = Math.max(2, knownPlayers.size() * 2);
        level.setBlock(worldPosition, getBlockState().setValue(RabbitHoleBlock.PRIMED, true), Block.UPDATE_CLIENTS);
        level.playSound(null, worldPosition, RabbitHoleContent.PRIME_SOUND, SoundSource.BLOCKS, 1.0F, 1.0F);
        setChanged();
    }

    public void teleport(ServerPlayer player) {
        if (!(level instanceof ServerLevel origin) || charges <= 0) {
            return;
        }
        ServerLevel destination = origin.dimension().equals(RabbitHoleContent.BURROW)
            ? origin.getServer().overworld()
            : origin.getServer().getLevel(RabbitHoleContent.BURROW);
        if (destination == null) {
            return;
        }
        charges--;
        if (charges == 0) {
            origin.setBlock(worldPosition, getBlockState().setValue(RabbitHoleBlock.PRIMED, false), Block.UPDATE_CLIENTS);
        }
        setChanged();
        BlockPos arrival;
        if (linkedPosition != Long.MIN_VALUE) {
            BlockPos linked = BlockPos.of(linkedPosition);
            arrival = destination.getBlockState(linked).is(RabbitHoleContent.BLOCK)
                ? linked
                : findArrival(destination, linked.getX(), linked.getZ());
        } else {
            double scale = origin.dimension().equals(RabbitHoleContent.BURROW) ? 1.0D / 3.0D : 3.0D;
            int targetX = (int)Math.floor(worldPosition.getX() * scale);
            int targetZ = (int)Math.floor(worldPosition.getZ() * scale);
            arrival = findArrival(destination, targetX, targetZ);
        }
		prepareBackside(destination, arrival);
		if (origin.dimension().equals(RabbitHoleContent.BURROW)) {
			GranulesAdvancements.leaveBurrow(player, arrival.getX(), arrival.getZ());
		} else {
			GranulesAdvancements.enterBurrow(player, worldPosition.getX(), worldPosition.getZ());
		}
        if (destination.getBlockEntity(arrival) instanceof RabbitHoleBlockEntity counterpart) {
            linkedPosition = arrival.asLong();
            counterpart.receiveLink(worldPosition, charges, knownPlayers);
            setChanged();
        }
        origin.playSound(null, worldPosition, RabbitHoleContent.ENTER_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.teleport(
            new TeleportTransition(
                destination,
                Vec3.atBottomCenterOf(arrival.above()),
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                TeleportTransition.DO_NOTHING
            )
        );
        destination.playSound(null, arrival, RabbitHoleContent.EMERGE_SOUND, SoundSource.PLAYERS, 0.25F, 1.0F);
    }

    private static BlockPos findArrival(ServerLevel destination, int x, int z) {
        int maximumY = destination.dimension().equals(RabbitHoleContent.BURROW)
            ? 120
            : destination.getMaxY() - 3;
        for (int radius = 0; radius <= 16; radius++) {
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (radius > 0 && Math.abs(offsetX) != radius && Math.abs(offsetZ) != radius) {
                        continue;
                    }
                    BlockPos arrival = findSurface(destination, x + offsetX, z + offsetZ, maximumY);
                    if (arrival != null) {
                        return arrival;
                    }
                }
            }
        }
        int platformY = destination.dimension().equals(RabbitHoleContent.BURROW) ? 64 : 80;
        BlockPos fallback = new BlockPos(x, platformY, z);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                destination.setBlock(fallback.offset(offsetX, -1, offsetZ), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                destination.removeBlock(fallback.offset(offsetX, 0, offsetZ), false);
                destination.removeBlock(fallback.offset(offsetX, 1, offsetZ), false);
            }
        }
        return fallback;
    }

    private static BlockPos findSurface(ServerLevel destination, int x, int z, int maximumY) {
        for (int y = maximumY; y > destination.getMinY() + 2; y--) {
            BlockPos ground = new BlockPos(x, y, z);
            BlockState state = destination.getBlockState(ground);
            if (!state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.PODZOL) && !state.is(Blocks.MYCELIUM)) {
                continue;
            }
            if (destination.getBlockState(ground.above()).isAir()
                && destination.getBlockState(ground.above(2)).isAir()) {
                return ground.above();
            }
        }
        return null;
    }

    private void prepareBackside(ServerLevel destination, BlockPos arrival) {
        if (!destination.getBlockState(arrival.below()).is(Blocks.GRASS_BLOCK)
            && !destination.getBlockState(arrival.below()).is(Blocks.PODZOL)
            && !destination.getBlockState(arrival.below()).is(Blocks.MYCELIUM)) {
            destination.setBlock(arrival.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
        destination.removeBlock(arrival.above(), false);
        destination.setBlock(
            arrival,
            RabbitHoleContent.BLOCK.defaultBlockState().setValue(RabbitHoleBlock.PRIMED, charges > 0),
            Block.UPDATE_ALL
        );
    }

    private void receiveLink(BlockPos otherPosition, int remainingCharges, Set<UUID> players) {
        linkedPosition = otherPosition.asLong();
        charges = remainingCharges;
        knownPlayers.addAll(players);
        if (level != null) {
            level.setBlock(
                worldPosition,
                getBlockState().setValue(RabbitHoleBlock.PRIMED, charges > 0),
                Block.UPDATE_CLIENTS
            );
        }
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RabbitHoleBlockEntity hole) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        hole.rememberPlayers(serverLevel);
        if (state.getValue(RabbitHoleBlock.PRIMED)) {
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                new AABB(pos).inflate(0.0D, 0.125D, 0.0D),
                player -> player.isShiftKeyDown() && player.onGround() && !player.isOnPortalCooldown()
            )) {
                player.setPortalCooldown();
                hole.teleport(player);
                return;
            }
        }
        long time = serverLevel.getGameTime();
        long day = time / 24000L;
        long dayTime = time % 24000L;
        if (dayTime < 13000L || dayTime >= 23000L || hole.lastSpawnNight == day) {
            return;
        }
        hole.lastSpawnNight = day;
        int phase = (int)(day % 8L);
        int count = phase == 0 || phase == 4 ? 2 : 1;
        for (int index = 0; index < count; index++) {
            Rabbit rabbit = EntityTypes.RABBIT.create(serverLevel, EntitySpawnReason.NATURAL);
            if (rabbit == null) {
                continue;
            }
            int x = pos.getX() + serverLevel.getRandom().nextInt(3) - 1;
            int z = pos.getZ() + serverLevel.getRandom().nextInt(3) - 1;
            rabbit.snapTo(x + 0.5D, pos.getY() + 0.1D, z + 0.5D, serverLevel.getRandom().nextFloat() * 360.0F, 0.0F);
            serverLevel.addFreshEntity(rabbit);
        }
        hole.setChanged();
    }

    private void rememberPlayers(ServerLevel level) {
        int previousSize = knownPlayers.size();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            knownPlayers.add(player.getUUID());
        }
        if (knownPlayers.size() != previousSize) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Charges", charges);
        output.putLong("LastSpawnNight", lastSpawnNight);
        output.putLong("LinkedPosition", linkedPosition);
        output.store("KnownPlayers", Codec.STRING.listOf(), knownPlayers.stream().map(UUID::toString).toList());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charges = input.getIntOr("Charges", 0);
        lastSpawnNight = input.getLongOr("LastSpawnNight", Long.MIN_VALUE);
        linkedPosition = input.getLongOr("LinkedPosition", Long.MIN_VALUE);
        knownPlayers.clear();
        for (String value : input.read("KnownPlayers", Codec.STRING.listOf()).orElse(java.util.List.of())) {
            knownPlayers.add(UUID.fromString(value));
        }
    }
}
