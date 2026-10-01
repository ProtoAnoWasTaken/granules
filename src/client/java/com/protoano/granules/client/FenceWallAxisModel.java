package com.protoano.granules.client;

import com.protoano.granules.block.FenceWallOrientation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class FenceWallAxisModel extends WrapperBlockStateModel {
    private final Direction.Axis axis;

    private FenceWallAxisModel(BlockStateModel model, Direction.Axis axis) {
        super(model);
        this.axis = axis;
    }

    public static void initialize() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bake) -> {
            BlockState state = bake.state();
            if ((state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock)
                && state.hasProperty(FenceWallOrientation.AXIS)
                && state.getValue(FenceWallOrientation.AXIS) != Direction.Axis.Y) {
                return new FenceWallAxisModel(model, state.getValue(FenceWallOrientation.AXIS));
            }
            return model;
        }));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        super.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            output.add(new AxisPart(part, axis));
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos position, BlockState state, RandomSource random) {
        Object sourceKey = super.createGeometryKey(level, position, state, random);
        if (sourceKey == null) {
            return null;
        }
        return new GeometryKey(axis, sourceKey);
    }

    private record GeometryKey(Direction.Axis axis, Object sourceKey) {
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos position, BlockState state,
        RandomSource random, Predicate<Direction> cullTest) {
        emitter.pushTransform(quad -> {
            Direction nominalFace = quad.nominalFace();
            Direction cullFace = quad.cullFace();
            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3f transformed = transform(new Vector3f(quad.x(vertex), quad.y(vertex), quad.z(vertex)), axis);
                quad.pos(vertex, transformed);
                if (quad.hasNormal(vertex)) {
                    Vector3f normal = rotate(new Vector3f(quad.normalX(vertex), quad.normalY(vertex), quad.normalZ(vertex)), axis);
                    quad.normal(vertex, normal);
                }
            }
            quad.nominalFace(FenceWallOrientation.toWorld(nominalFace, axis));
            quad.cullFace(FenceWallOrientation.toWorld(cullFace, axis));
            return true;
        });
        try {
            super.emitQuads(emitter, level, position, state, random,
                direction -> cullTest.test(FenceWallOrientation.toWorld(direction, axis)));
        } finally {
            emitter.popTransform();
        }
    }

    private record AxisPart(BlockStateModelPart source, Direction.Axis axis) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : source.getQuads(FenceWallOrientation.toLocal(direction, axis))) {
                result.add(new BakedQuad(
                    transform(quad.position0(), axis),
                    transform(quad.position1(), axis),
                    transform(quad.position2(), axis),
                    transform(quad.position3(), axis),
                    quad.packedUV0(),
                    quad.packedUV1(),
                    quad.packedUV2(),
                    quad.packedUV3(),
                    FenceWallOrientation.toWorld(quad.direction(), axis),
                    quad.materialInfo()
                ));
            }
            return result;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return source.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return source.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return source.materialFlags();
        }
    }

    private static Vector3f transform(Vector3fc position, Direction.Axis axis) {
        Vector3f centered = new Vector3f(position).sub(0.5F, 0.5F, 0.5F);
        Vector3f rotated = rotate(centered, axis);
        return rotated.add(0.5F, 0.5F, 0.5F);
    }

    private static Vector3f rotate(Vector3f vector, Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return new Vector3f(vector.y, -vector.x, vector.z);
        }
        if (axis == Direction.Axis.Z) {
            return new Vector3f(vector.x, -vector.z, vector.y);
        }
        return vector;
    }

}
