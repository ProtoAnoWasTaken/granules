package com.puppy.granules.rabbit;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import com.puppy.granules.pet.PetBedService;

import java.util.UUID;

public class RabbitGameTests {
    @GameTest
    public void killerBunnyEggCreatesExcludedKillerRabbit(GameTestHelper helper) {
        ItemStack egg = new ItemStack(RabbitContent.KILLER_BUNNY_SPAWN_EGG);
        require(egg.get(DataComponents.RABBIT_VARIANT) == Rabbit.Variant.EVIL, "Killer Bunny egg must carry the evil rabbit variant");
        Rabbit rabbit = EntityTypes.RABBIT.create(helper.getLevel(), EntitySpawnReason.SPAWN_ITEM_USE);
        require(rabbit != null, "Killer Bunny egg must create a Rabbit");
        rabbit.finalizeSpawn(
            helper.getLevel(),
            helper.getLevel().getCurrentDifficultyAt(rabbit.blockPosition()),
            EntitySpawnReason.SPAWN_ITEM_USE,
            null
        );
        rabbit.applyComponentsFromItemStack(egg);
        require(rabbit.getVariant() == Rabbit.Variant.EVIL, "Killer Bunny egg must produce the Killer Bunny variant");
        require(rabbit.getCustomName() == null, "Killer Bunnies must not retain Minecraft's automatic nameplate");
        require(!((KillerRabbitAccess) rabbit).granules$dropsPalePelt(), "Spawn-egged Killer Bunnies must not qualify for Pale Pelts");
        helper.succeed();
    }

    @GameTest
    public void killerRabbitRetainsPlayerAssignedNames(GameTestHelper helper) {
        Rabbit rabbit = EntityTypes.RABBIT.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        require(rabbit != null, "Command summon must create a Rabbit");
        rabbit.setCustomName(Component.literal("Toast"));
        rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        require(rabbit.getCustomName() != null, "Killer Rabbits must retain player-assigned names");
        require(rabbit.getCustomName().getString().equals("Toast"), "Killer Rabbits must retain the exact player-assigned name");
        helper.succeed();
    }

    @GameTest
    public void commandSummonedKillerRabbitQualifiesForPalePelts(GameTestHelper helper) {
        Rabbit rabbit = EntityTypes.RABBIT.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        require(rabbit != null, "Command summon must create a Rabbit");
        rabbit.finalizeSpawn(
            helper.getLevel(),
            helper.getLevel().getCurrentDifficultyAt(rabbit.blockPosition()),
            EntitySpawnReason.COMMAND,
            null
        );
        rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        require(((KillerRabbitAccess) rabbit).granules$dropsPalePelt(), "Command-summoned Killer Bunnies must qualify for Pale Pelts");
        helper.succeed();
    }

    @GameTest
    public void petBedUsesKillerBunnyEggForTamedKillerRabbit(GameTestHelper helper) {
        PetBedService.PetEntry entry = new PetBedService.PetEntry(
            UUID.randomUUID(),
            "Killer Bunny",
            EntityTypes.RABBIT,
            false,
            false,
            true,
            true
        );
        require(PetBedService.iconFor(entry).is(RabbitContent.KILLER_BUNNY_SPAWN_EGG), "Pet Beds must represent tamed Killer Rabbits with the Killer Bunny spawn egg");
        helper.succeed();
    }

    @GameTest
    public void killerRabbitDeathLootSubstitutesPalePelts(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        for (int index = 0; index < 64; index++) {
            Rabbit rabbit = helper.spawn(EntityTypes.RABBIT, 4, 2, 4, EntitySpawnReason.COMMAND);
            rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
            if (index % 2 == 0) {
                ((KillerRabbitAccess) rabbit).granules$setOwnerUuid(player.getUUID());
            }
            rabbit.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000.0F);
        }
        require(helper.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).stream()
            .noneMatch(entity -> entity.getItem().is(net.minecraft.world.item.Items.RABBIT_HIDE)), "Eligible wild and tamed Killer Bunnies must not drop Rabbit Hide");
        require(helper.getEntities(net.minecraft.world.entity.EntityTypes.ITEM).stream()
            .anyMatch(entity -> entity.getItem().is(RabbitContent.PALE_PELT)), "Eligible wild and tamed Killer Bunnies must drop Pale Pelts in place of Rabbit Hide");
        helper.succeed();
    }

    @GameTest
    public void tamedKillerRabbitCannotTargetOrDamageOwner(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        Rabbit rabbit = helper.spawn(EntityTypes.RABBIT, 4, 2, 4, EntitySpawnReason.COMMAND);
        rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        double untamedHealth = rabbit.getAttributeValue(Attributes.MAX_HEALTH);
        double untamedArmor = rabbit.getAttributeValue(Attributes.ARMOR);
        ((KillerRabbitAccess) rabbit).granules$setOwnerUuid(owner.getUUID());
        require(close(rabbit.getAttributeValue(Attributes.MAX_HEALTH), untamedHealth * 2.0D), "Taming must double Killer Rabbit maximum health");
        require(close(rabbit.getAttributeValue(Attributes.ARMOR), untamedArmor * 2.0D), "Taming must double Killer Rabbit armor");
        require(close(rabbit.getHealth(), rabbit.getMaxHealth()), "Taming must fill the Killer Rabbit's increased health");
        rabbit.setTarget(owner);
        require(rabbit.getTarget() == null, "A tamed Killer Rabbit must reject its owner as a target");
        require(!rabbit.canAttack(owner), "A tamed Killer Rabbit must not consider its owner attackable");
        float ownerHealth = owner.getHealth();
        require(!rabbit.doHurtTarget(helper.getLevel(), owner), "A tamed Killer Rabbit must cancel damage against its owner");
        require(owner.getHealth() == ownerHealth, "A tamed Killer Rabbit must not damage its owner");
        rabbit.setTarget(rabbit);
        require(rabbit.getTarget() == null, "A tamed Killer Rabbit must reject itself as a target");
        float rabbitHealth = rabbit.getHealth();
        require(!rabbit.doHurtTarget(helper.getLevel(), rabbit), "A tamed Killer Rabbit must cancel damage against itself");
        require(rabbit.getHealth() == rabbitHealth, "A tamed Killer Rabbit must not damage itself");
        var unrelated = helper.spawn(EntityTypes.ZOMBIE, 6, 2, 4, EntitySpawnReason.COMMAND);
        ((KillerRabbitAccess) rabbit).granules$toggleSitting(owner);
        rabbit.getNavigation().moveTo(unrelated, 1.0D);
        rabbit.getGoalSelector().tick();
        rabbit.setTarget(unrelated);
        require(((KillerRabbitAccess) rabbit).granules$isOrderedToSit(), "A Killer Rabbit's sit command must remain active");
        require(rabbit.getNavigation().isDone(), "A sitting Killer Rabbit must stop its navigation continuously");
        require(rabbit.getTarget() == null, "A sitting Killer Rabbit must reject unrelated hostile targets");
        ((KillerRabbitAccess) rabbit).granules$toggleSitting(owner);
        require(!((KillerRabbitAccess) rabbit).granules$isOrderedToSit(), "A second owner interaction must release a sitting Killer Rabbit");
        helper.succeed();
    }

    @GameTest
    public void furBootsProvideRequestedAttributes(GameTestHelper helper) {
        ItemStack boots = new ItemStack(PalePeltEquipment.FUR_BOOTS);
        var attributes = boots.get(DataComponents.ATTRIBUTE_MODIFIERS);
        require(attributes != null, "Fur Boots must provide equipment attributes");
        require(close(attributes.compute(Attributes.MOVEMENT_SPEED, 0.1, EquipmentSlot.FEET), 0.1), "Fur Boots must not change movement speed");
        require(close(attributes.compute(Attributes.JUMP_STRENGTH, 0.42, EquipmentSlot.FEET), 0.525), "Fur Boots must add 25 percent jump strength");
        require(close(attributes.compute(Attributes.SAFE_FALL_DISTANCE, 3.0, EquipmentSlot.FEET), 3.75), "Fur Boots must add matching safe fall distance");
        require(close(attributes.compute(Attributes.AIR_DRAG_MODIFIER, 1.0, EquipmentSlot.FEET), 0.0), "Fur Boots must remove air drag");
        require(net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(equippedPlayer(helper, EquipmentSlot.FEET, boots)), "Fur Boots must prevent falling through Powder Snow");
        helper.succeed();
    }

    @GameTest
    public void torchflowersEmitSevenLight(GameTestHelper helper) {
        require(Blocks.TORCHFLOWER.defaultBlockState().getLightEmission() == 7, "Torchflowers must emit light level seven");
        helper.succeed();
    }

    @GameTest
    public void rabbitsOnlyTakeFallDamageOnPointedDripstone(GameTestHelper helper) {
        Rabbit rabbit = helper.spawn(EntityTypes.RABBIT, 4, 3, 4, EntitySpawnReason.COMMAND);
        float initialHealth = rabbit.getHealth();
        boolean damagedOnStone = rabbit.causeFallDamage(10.0D, 1.0F, helper.getLevel().damageSources().fall());
        require(!damagedOnStone && rabbit.getHealth() == initialHealth, "Rabbits must ignore ordinary fall damage");
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.POINTED_DRIPSTONE);
        boolean damagedOnDripstone = rabbit.causeFallDamage(10.0D, 1.0F, helper.getLevel().damageSources().fall());
        require(damagedOnDripstone && rabbit.getHealth() < initialHealth, "Pointed Dripstone must still inflict rabbit fall damage");
        helper.succeed();
    }

    @GameTest
    public void rabbitHoleRequiresCompleteMushroomRing(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(center.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        helper.getLevel().setBlock(center, RabbitHoleContent.BLOCK.defaultBlockState(), 3);
        require(!RabbitHoleBlock.hasMushroomRing(helper.getLevel(), center), "An incomplete mushroom ring must not prime a Rabbit Hole");
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if ((Math.abs(x) == 2 || Math.abs(z) == 2) && !(Math.abs(x) == 2 && Math.abs(z) == 2)) {
                    helper.getLevel().setBlock(center.offset(x, -1, z), Blocks.PODZOL.defaultBlockState(), 3);
                    helper.getLevel().setBlock(center.offset(x, 0, z), Blocks.BROWN_MUSHROOM.defaultBlockState(), 3);
                }
            }
        }
        require(RabbitHoleBlock.hasMushroomRing(helper.getLevel(), center), "Twelve red or brown mushrooms must complete the Rabbit Hole ring");
        helper.succeed();
    }

    private static net.minecraft.server.level.ServerPlayer equippedPlayer(GameTestHelper helper, EquipmentSlot slot, ItemStack stack) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setItemSlot(slot, stack);
        return player;
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.0001;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
