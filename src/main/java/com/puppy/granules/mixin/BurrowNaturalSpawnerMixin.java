package com.puppy.granules.mixin;

import com.puppy.granules.rabbit.RabbitHoleContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class BurrowNaturalSpawnerMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void granules$preserveBurrowCaveMobCap(
        ServerLevel level,
        MobCategory category,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        MobSpawnSettings.SpawnerData spawnData,
        BlockPos.MutableBlockPos pos,
        double distance,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (
            level.dimension().equals(RabbitHoleContent.BURROW)
                && category == MobCategory.MONSTER
                && pos.getY() >= 48
                && level.getRandom().nextInt(3) != 0
        ) {
            callback.setReturnValue(false);
        }
    }
}
