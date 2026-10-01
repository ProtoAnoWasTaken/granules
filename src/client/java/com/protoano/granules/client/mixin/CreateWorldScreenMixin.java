package com.protoano.granules.client.mixin;

import com.protoano.granules.world.StructureFrequency;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	@Shadow
	public abstract net.minecraft.client.gui.screens.worldselection.WorldCreationUiState getUiState();

	@Unique
	private final Map<AbstractWidget, Integer> granules$originalY = new IdentityHashMap<>();

	@Unique
	private CycleButton<Factor> granules$factorButton;

	@Unique
	private StringWidget granules$factorLabel;

	@Inject(method = "init", at = @At("TAIL"))
	private void addStructureFrequencyControl(CallbackInfo callbackInfo) {
		CreateWorldScreen screen = (CreateWorldScreen) (Object) this;
		this.granules$factorButton = CycleButton.builder(Factor::label, Factor.fromMultiplier(StructureFrequency.multiplier()))
			.withValues(List.of(Factor.TWO, Factor.ONE, Factor.HALF, Factor.QUARTER))
			.displayOnlyValue()
			.create(
				0,
				0,
				44,
				20,
				Component.empty(),
				(button, factor) -> StructureFrequency.setMultiplier(factor.multiplier())
			);
		this.granules$factorLabel = new StringWidget(Component.literal("Generation Factor"), screen.getFont());
		this.granules$factorButton.visible = false;
		this.granules$factorLabel.visible = false;
		ScreenAccessor accessor = (ScreenAccessor) (Object) this;
		accessor.granules$addRenderableWidget(this.granules$factorButton);
		accessor.granules$addRenderableOnly(this.granules$factorLabel);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void positionStructureFrequencyControl(
		GuiGraphicsExtractor graphics,
		int mouseX,
		int mouseY,
		float delta,
		CallbackInfo callbackInfo
	) {
		if (this.granules$factorButton == null || this.granules$factorLabel == null) {
			return;
		}
		List<? extends CycleButton<?>> switches = ((ScreenAccessor) (Object) this).granules$getRenderables().stream()
			.filter(CycleButton.class::isInstance)
			.map(widget -> (CycleButton<?>) widget)
			.filter(widget -> widget != this.granules$factorButton)
			.filter(widget -> widget.visible && widget.getWidth() == 44)
			.sorted(Comparator.comparingInt(AbstractWidget::getY))
			.toList();
		if (switches.size() < 2) {
			this.granules$factorButton.visible = false;
			this.granules$factorLabel.visible = false;
			return;
		}
		CycleButton<?> structuresButton = switches.get(0);
		CycleButton<?> bonusChestButton = switches.get(1);
		StringWidget bonusChestLabel = this.granules$findLabel(bonusChestButton);
		if (bonusChestLabel == null) {
			this.granules$factorButton.visible = false;
			this.granules$factorLabel.visible = false;
			return;
		}
		this.granules$rememberY(bonusChestButton);
		this.granules$factorButton.setX(bonusChestButton.getX());
		this.granules$factorButton.setY(this.granules$originalY.get(bonusChestButton));
		this.granules$factorLabel.setX(bonusChestLabel.getX());
		bonusChestButton.setY(this.granules$originalY.get(bonusChestButton) + 24);
		this.granules$factorLabel.setY(this.granules$factorButton.getY() + 5);
		bonusChestLabel.setY(bonusChestButton.getY() + 5);
		boolean structuresEnabled = structuresButton.getValue() instanceof Boolean enabled && enabled;
		this.granules$factorButton.active = structuresEnabled;
		if (structuresEnabled) {
			if (this.granules$factorButton.getValue() == Factor.ZERO) {
				this.granules$factorButton.setValue(Factor.ONE);
				StructureFrequency.setMultiplier(Factor.ONE.multiplier());
			}
		} else {
			this.granules$factorButton.setValue(Factor.ZERO);
			StructureFrequency.setMultiplier(Factor.ZERO.multiplier());
		}
		this.granules$factorButton.visible = true;
		this.granules$factorLabel.visible = true;
	}

	@Unique
	private StringWidget granules$findLabel(CycleButton<?> button) {
		return ((ScreenAccessor) (Object) this).granules$getRenderables().stream()
			.filter(StringWidget.class::isInstance)
			.map(StringWidget.class::cast)
			.filter(widget -> widget != this.granules$factorLabel)
			.filter(widget -> widget.getX() < button.getX())
			.filter(widget -> Math.abs(widget.getY() - button.getY()) <= 8)
			.min(Comparator.comparingInt(widget -> Math.abs(widget.getY() - button.getY())))
			.orElse(null);
	}

	@Unique
	private void granules$rememberY(AbstractWidget widget) {
		this.granules$originalY.putIfAbsent(widget, widget.getY());
	}

	private enum Factor {
		ZERO(0.0D, "0x"),
		TWO(2.0D, "2x"),
		ONE(1.0D, "1x"),
		HALF(0.5D, "0.5x"),
		QUARTER(0.25D, "0.25x");

		private final double multiplier;
		private final String text;

		Factor(double multiplier, String text) {
			this.multiplier = multiplier;
			this.text = text;
		}

		private double multiplier() {
			return this.multiplier;
		}

		private Component label() {
			return Component.literal(this.text);
		}

		private static Factor fromMultiplier(double multiplier) {
			for (Factor factor : values()) {
				if (factor.multiplier == multiplier) {
					return factor;
				}
			}
			return ONE;
		}
	}
}
