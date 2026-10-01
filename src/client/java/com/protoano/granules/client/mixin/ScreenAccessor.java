package com.protoano.granules.client.mixin;

import java.util.List;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
	@Invoker("addRenderableWidget")
	GuiEventListener granules$addRenderableWidget(GuiEventListener widget);

	@Invoker("addRenderableOnly")
	Renderable granules$addRenderableOnly(Renderable renderable);

	@Accessor("renderables")
	List<Renderable> granules$getRenderables();
}
