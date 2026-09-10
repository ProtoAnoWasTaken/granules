package com.puppy.granules.compat;

import com.puppy.granules.disc.DiscBurnerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

@WailaPlugin("granules")
public class DiscBurnerJadePlugin implements IWailaPlugin {
    public static final Identifier UID = Identifier.fromNamespaceAndPath("granules", "disc_burner");
    public static final String DATA_KEY = "granules:disc_burner";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(KillerRabbitJadeProvider.INSTANCE, Rabbit.class);
        registration.registerBlockDataProvider(new IServerDataProvider<BlockAccessor>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                if (!(accessor.getBlockEntity() instanceof DiscBurnerBlockEntity burner)) {
                    return;
                }
                CompoundTag status = new CompoundTag();
                status.put("Source", accessor.encodeAsNbt(ItemStack.OPTIONAL_STREAM_CODEC, burner.getSourceDisc()));
                status.put("Target", accessor.encodeAsNbt(ItemStack.OPTIONAL_STREAM_CODEC, burner.getTargetDisc()));
                status.putBoolean("Burning", burner.isBurning());
                status.putInt("Progress", burner.getCopyProgress());
                status.putInt("Duration", burner.getCopyDuration());
                data.put(DATA_KEY, status);
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        }, DiscBurnerBlockEntity.class);
    }
}
