package com.puppy.granules.bomb;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BombGameTests {
    @GameTest
    public void bombsStackToSixteen(GameTestHelper helper) {
        require(new ItemStack(BombContent.DYNAMITE).getMaxStackSize() == 16, "Dynamite must stack to 16");
        require(new ItemStack(BombContent.DIRT_BOMB).getMaxStackSize() == 16, "Dirt Bombs must stack to 16");
        require(new ItemStack(BombContent.DRY_BOMB).getMaxStackSize() == 16, "Dry Bombs must stack to 16");
        helper.succeed();
    }

    @GameTest
    public void dispensersLaunchEveryBombAtHalfCharge(GameTestHelper helper) {
        require(DispenserBlock.DISPENSER_REGISTRY.containsKey(BombContent.DYNAMITE), "Dispenser must launch Dynamite");
        require(DispenserBlock.DISPENSER_REGISTRY.containsKey(BombContent.DIRT_BOMB), "Dispenser must launch Dirt Bombs");
        require(DispenserBlock.DISPENSER_REGISTRY.containsKey(BombContent.DRY_BOMB), "Dispenser must launch Dry Bombs");
        verifyDispenseConfig(BombContent.DYNAMITE);
        verifyDispenseConfig(BombContent.DIRT_BOMB);
        verifyDispenseConfig(BombContent.DRY_BOMB);
        helper.succeed();
    }

    @GameTest
    public void dirtBombWaitsFourSecondsAndProtectsSpecialBlocks(GameTestHelper helper) {
        BlockPos stone = new BlockPos(5, 2, 5);
        BlockPos workstation = stone.east();
        BlockPos container = stone.west();
        BlockPos air = stone.north();
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(workstation, Blocks.FLETCHING_TABLE);
        helper.setBlock(container, Blocks.CHEST);
        BombProjectile bomb = planted(helper, BombContent.DIRT_BOMB, stone.above());
        for (int tick = 0; tick < 79; tick++) {
            bomb.tick();
        }
        require(helper.getBlockState(stone).is(Blocks.STONE), "Dirt Bomb must wait 80 ticks before changing terrain");
        bomb.tick();
        require(helper.getBlockState(stone).is(Blocks.DIRT), "Dirt Bomb must replace ordinary blocks with dirt");
        require(helper.getBlockState(air).is(Blocks.DIRT), "Dirt Bomb must fill air in its radius with dirt");
        require(helper.getBlockState(workstation).is(Blocks.FLETCHING_TABLE), "Dirt Bomb must preserve workstations");
        require(helper.getBlockState(container).is(Blocks.CHEST), "Dirt Bomb must preserve containers");
        helper.succeed();
    }

    @GameTest
    public void dryBombRemovesSourceFlowingAndWaterloggedLiquids(GameTestHelper helper) {
        BlockPos source = new BlockPos(5, 2, 5);
        BlockPos flowing = source.east();
        BlockPos waterlogged = source.west();
        BlockPos solid = source.north();
        helper.setBlock(source, Blocks.WATER);
        helper.setBlock(flowing, Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 4));
        helper.setBlock(waterlogged, Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        helper.setBlock(solid, Blocks.STONE);
        BombProjectile bomb = planted(helper, BombContent.DRY_BOMB, source.above());
        for (int tick = 0; tick < 80; tick++) {
            bomb.tick();
        }
        require(helper.getBlockState(source).isAir(), "Dry Bomb must remove source liquids");
        require(helper.getBlockState(flowing).isAir(), "Dry Bomb must remove flowing liquids");
        require(helper.getBlockState(waterlogged).is(Blocks.OAK_SLAB), "Dry Bomb must preserve waterlogged blocks");
        require(!helper.getBlockState(waterlogged).getValue(BlockStateProperties.WATERLOGGED), "Dry Bomb must dry waterlogged blocks");
        require(helper.getBlockState(solid).is(Blocks.STONE), "Dry Bomb must preserve solid blocks");
        helper.succeed();
    }

    private static BombProjectile planted(GameTestHelper helper, net.minecraft.world.item.Item item, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BombProjectile bomb = new BombProjectile(
            helper.getLevel(),
            absolute.getX() + 0.5,
            absolute.getY() + 0.5,
            absolute.getZ() + 0.5,
            new ItemStack(item)
        );
        helper.getLevel().addFreshEntity(bomb);
        bomb.onHitBlock(new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        return bomb;
    }

    private static void verifyDispenseConfig(BombItem item) {
        BombItem.DispenseConfig config = item.createDispenseConfig();
        require(Math.abs(config.power() - BombItem.HALF_CHARGE_POWER) < 0.0001F, "Dispenser bomb speed must match a half-charged throw");
        require(config.uncertainty() == 1.0F, "Dispenser bomb accuracy must match snowballs");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
