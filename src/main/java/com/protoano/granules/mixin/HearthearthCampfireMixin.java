package com.protoano.granules.mixin;

import com.protoano.granules.enchantment.HearthearthCampfire;
import com.protoano.granules.enchantment.HearthearthCampfireAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlockEntity.class)
public abstract class HearthearthCampfireMixin implements HearthearthCampfireAccess {
    @Unique
    private final HearthearthCampfire granules$plantedSwords = new HearthearthCampfire();

    @Override
    public HearthearthCampfire granules$hearthearth() {
        return granules$plantedSwords;
    }

    @Inject(method = "cookTick", at = @At("TAIL"))
    private static void repairSwords(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity campfire,
                                    RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes,
                                    CallbackInfo callback) {
        HearthearthCampfire.get(campfire).tick(campfire);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void loadSwords(ValueInput input, CallbackInfo callback) {
        granules$plantedSwords.load(input);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void saveSwords(ValueOutput output, CallbackInfo callback) {
        granules$plantedSwords.save(output);
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void syncSwords(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> callback) {
        callback.getReturnValue().store(HearthearthCampfire.DATA_KEY, HearthearthCampfire.PlantedSword.CODEC.listOf(),
            registries.createSerializationContext(NbtOps.INSTANCE), granules$plantedSwords.swords());
    }

    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
    private void dropSwords(BlockPos pos, BlockState state, CallbackInfo callback) {
        granules$plantedSwords.dropAll((CampfireBlockEntity) (Object) this);
    }
}
