package com.protoano.granules.mixin;

import com.protoano.granules.GranulesMod;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerChunkCache.class)
public abstract class PassiveAnimalSpawningMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @ModifyArg(
        method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getFilteredSpawningCategories(Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZ)Ljava/util/List;"),
        index = 2
    )
    private boolean granules$enableRegularAnimalSpawning(boolean spawnPersistent) {
        return spawnPersistent || this.level.getGameRules().get(GranulesMod.LEGACY_PASSIVE_MOBS);
    }
}
