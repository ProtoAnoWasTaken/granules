package com.puppy.granules.mixin;

import com.puppy.granules.world.HoneyloggingProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StateDefinition.Builder.class)
public abstract class StateDefinitionBuilderHoneyMixin {
	@Inject(method = "add", at = @At("HEAD"))
	private void granules$addHoneyloggedProperty(Property<?>[] properties, CallbackInfoReturnable<StateDefinition.Builder<?, ?>> callbackInfo) {
		for (Property<?> property : properties) {
			if (property == BlockStateProperties.WATERLOGGED) {
				((StateDefinition.Builder<?, ?>) (Object) this).add(HoneyloggingProperties.HONEYLOGGED);
				return;
			}
		}
	}
}
