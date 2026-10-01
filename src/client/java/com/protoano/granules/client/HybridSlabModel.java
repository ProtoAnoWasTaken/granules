package com.protoano.granules.client;

import com.protoano.granules.block.HybridSlabBlockEntity;
import com.protoano.granules.block.HybridSlabs;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3f;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.BlockState;

public final class HybridSlabModel extends WrapperBlockStateModel {
    private final HybridSlabBlockEntity.Halves fixedHalves;

    private HybridSlabModel(BlockStateModel fallback, HybridSlabBlockEntity.Halves fixedHalves) {
        super(fallback);
        this.fixedHalves = fixedHalves;
    }

    public static BlockStateModel preview(BlockStateModel fallback, HybridSlabBlockEntity.Halves halves) {
        return new HybridSlabModel(fallback, halves);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        if (fixedHalves == null) {
            super.collectParts(random, output);
            return;
        }
        for (boolean upper : new boolean[] {false, true}) {
            BlockState state = upper ? fixedHalves.top() : fixedHalves.bottom();
            List<BlockStateModelPart> parts = new ArrayList<>();
            model(state.setValue(SlabBlock.TYPE, SlabType.BOTTOM)).collectParts(random, parts);
            for (BlockStateModelPart part : parts) {
                output.add(new SlabPart(part, upper));
            }
        }
    }

    private record SlabPart(BlockStateModelPart source, boolean upper) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : source.getQuads(direction)) {
                float offset = upper ? 0.5F : 0.0F;
                boolean internal = true;
                for (int vertex = 0; vertex < 4; vertex++) {
                    internal &= Math.abs(quad.position(vertex).y() + offset - 0.5F) < 0.0001F;
                }
                if (internal) {
                    continue;
                }
                if (upper) {
                    result.add(new BakedQuad(new Vector3f(quad.position0()).add(0.0F, offset, 0.0F),
                        new Vector3f(quad.position1()).add(0.0F, offset, 0.0F),
                        new Vector3f(quad.position2()).add(0.0F, offset, 0.0F),
                        new Vector3f(quad.position3()).add(0.0F, offset, 0.0F),
                        quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo()));
                } else {
                    result.add(quad);
                }
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

    public static void initialize() {
        ModelLoadingPlugin.register(context -> {
            Set<Identifier> sharedModels = ConcurrentHashMap.newKeySet();
            context.modifyBlockModelOnLoad().register((model, load) -> {
                if (!(load.state().getBlock() instanceof SlabBlock)) {
                    model.resolveDependencies(sharedModels::add);
                    Identifier blockId = BuiltInRegistries.BLOCK.getKey(load.state().getBlock());
                    sharedModels.add(Identifier.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath()));
                }
                return model;
            });
            context.modifyBlockModelAfterBake().register((model, bake) -> {
                if (bake.state().is(HybridSlabs.BLOCK)) {
                    return new HybridSlabModel(model, null);
                }
                BlockState state = bake.state();
                if (state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                    Set<Identifier> dependencies = new HashSet<>();
                    bake.sourceModel().resolveDependencies(dependencies::add);
                    if (!dependencies.isEmpty() && sharedModels.containsAll(dependencies)) {
                        BlockState bottom = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                        return new HybridSlabModel(model, new HybridSlabBlockEntity.Halves(bottom, bottom));
                    }
                }
                return model;
            });
        });
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos position, BlockState state,
        RandomSource random, Predicate<Direction> cullTest) {
        if (halves(level, position) instanceof HybridSlabBlockEntity.Halves pair) {
            emitHalf(emitter, level, position, pair.bottom(), random, cullTest, false);
            emitHalf(emitter, level, position, pair.top(), random, cullTest, true);
        } else {
            super.emitQuads(emitter, level, position, state, random, cullTest);
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos position, BlockState state, RandomSource random) {
        return halves(level, position);
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos position, BlockState state) {
        if (halves(level, position) instanceof HybridSlabBlockEntity.Halves pair) {
            return model(pair.top()).particleMaterial();
        }
        return super.particleMaterial(level, position, state);
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos position, BlockState state, RandomSource random) {
        if (halves(level, position) instanceof HybridSlabBlockEntity.Halves pair) {
            return model(pair.bottom()).materialFlags() | model(pair.top()).materialFlags();
        }
        return super.materialFlags(level, position, state, random);
    }

    private static void emitHalf(QuadEmitter emitter, BlockAndTintGetter level, BlockPos position,
        BlockState state, RandomSource random, Predicate<Direction> cullTest, boolean upper) {
        BlockState bottom = state.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        emitter.pushTransform(quad -> {
            if (upper) {
                quad.translate(0.0F, 0.5F, 0.0F);
                if (quad.nominalFace() == Direction.UP) {
                    quad.cullFace(Direction.UP);
                }
            }
            if (quad.nominalFace() == Direction.UP || quad.nominalFace() == Direction.DOWN) {
                boolean internal = true;
                for (int vertex = 0; vertex < 4; vertex++) {
                    internal &= Math.abs(quad.y(vertex) - 0.5F) < 0.0001F;
                }
                if (internal) {
                    return false;
                }
            }
            return true;
        });
        try {
            ((FabricBlockStateModel) model(bottom)).emitQuads(emitter, level, position, bottom, random,
                direction -> direction != (upper ? Direction.DOWN : Direction.UP) && cullTest.test(direction));
        } finally {
            emitter.popTransform();
        }
    }

    private Object halves(BlockAndTintGetter level, BlockPos position) {
        if (fixedHalves != null) {
            return fixedHalves;
        }
        return ((FabricBlockGetter) level).getBlockEntityRenderData(position);
    }

    private static BlockStateModel model(BlockState state) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
    }
}
