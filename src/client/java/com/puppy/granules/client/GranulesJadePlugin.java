package com.puppy.granules.client;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.block.PlanterBlock;
import com.puppy.granules.block.PlanterBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

@WailaPlugin("granules")
public class GranulesJadePlugin implements IWailaPlugin {
	private static final Identifier PLANTER_GROWTH = Identifier.fromNamespaceAndPath(GranulesMod.MOD_ID, "planter_growth");
	private static final IBlockComponentProvider PLANTER_GROWTH_PROVIDER = new IBlockComponentProvider() {
		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!(accessor.getBlockEntity() instanceof PlanterBlockEntity planter)) {
				return;
			}
			int maximum = planter.getMaximumGrowthAge();
			if (maximum <= 0) {
				return;
			}
			int percentage = Math.round(100.0F * planter.getGrowthAge() / maximum);
			tooltip.add(Component.translatable("jade.granules.planter_growth", percentage));
		}

		@Override
		public Identifier getUid() {
			return PLANTER_GROWTH;
		}
	};

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerEntityComponent(KillerRabbitJadeComponentProvider.INSTANCE, Rabbit.class);
		registration.registerBlockComponent(new DiscBurnerJadeProvider(), com.puppy.granules.disc.DiscBurnerBlock.class);
		registration.registerBlockComponent(PLANTER_GROWTH_PROVIDER, PlanterBlock.class);
		registration.registerCustomEnchantPower(GranulesMod.PHILOSOPHER, (state, level, position) -> 5.0F);
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof CandleBlock) {
				registration.registerCustomEnchantPower(block, GranulesJadePlugin::getCandleEnchantPower);
			}
		}
	}

	private static float getCandleEnchantPower(BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos position) {
		if (!state.getValue(CandleBlock.LIT)) {
			return 0.0F;
		}
		return state.getValue(CandleBlock.CANDLES) * 0.25F;
	}
}
