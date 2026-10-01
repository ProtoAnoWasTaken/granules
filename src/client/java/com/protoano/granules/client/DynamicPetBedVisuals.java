package com.protoano.granules.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.protoano.granules.pet.DynamicPetBeds;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class DynamicPetBedVisuals implements SpecialModelRenderer<DynamicPetBedVisuals.Parts> {
    public record Parts(ItemStackRenderState wood, ItemStackRenderState wool) {
    }

    public static Parts extract(DynamicPetBeds.Materials materials) {
        var client = Minecraft.getInstance();
        var wood = new ItemStackRenderState();
        var wool = new ItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(wood, new ItemStack(materials.woodItem()), ItemDisplayContext.NONE,
            client.level, null, 0);
        client.getItemModelResolver().updateForTopItem(wool, new ItemStack(materials.woolItem()), ItemDisplayContext.NONE,
            client.level, null, 0);
        return new Parts(wood, wool);
    }

    public static void render(Parts parts, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline) {
        part(parts.wool(), pose, collector, light, overlay, outline, 0, 0, 0, 0.9375F, 0.0625F, 0.9375F);
        part(parts.wood(), pose, collector, light, overlay, outline, 0, 0, 0.9375F, 1, 0.125F, 0.0625F);
        part(parts.wood(), pose, collector, light, overlay, outline, 0.9375F, 0, 0, 0.0625F, 0.125F, 0.9375F);
    }

    private static void part(ItemStackRenderState model, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                             int outline, float x, float y, float z, float width, float height, float depth) {
        pose.pushPose();
        pose.translate(x + width / 2, y + height / 2, z + depth / 2);
        pose.scale(width, height, depth);
        model.submit(pose, collector, light, overlay, outline);
        pose.popPose();
    }

    @Override
    public void submit(Parts parts, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
        render(parts, pose, collector, light, overlay, outline);
    }

    @Override
    public Parts extractArgument(ItemStack stack) {
        return extract(DynamicPetBeds.materials(stack));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(0, 0, 0));
        consumer.accept(new Vector3f(1, 0.125F, 1));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Parts> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Parts> bake(BakingContext context) {
            return new DynamicPetBedVisuals();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
