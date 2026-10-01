package com.protoano.granules.pet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.protoano.granules.GranulesMod;
import com.protoano.granules.block.PetBedBlock;
import com.protoano.granules.block.PetBedWood;
import com.protoano.granules.config.ContentManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class DynamicPetBeds {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("granules", "dynamic_pet_bed");
    public static final DynamicBlock BLOCK = Registry.register(BuiltInRegistries.BLOCK, ID,
        new DynamicBlock(BlockBehaviour.Properties.ofFullCopy(GranulesMod.PET_BED).setId(ResourceKey.create(Registries.BLOCK, ID))));
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ID,
        new BlockItem(BLOCK, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).useBlockDescriptionPrefix()));
    public static final BlockEntityType<BedEntity> ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ID,
        new BlockEntityType<>(BedEntity::new, Set.of(BLOCK)));

    private DynamicPetBeds() {
    }

    public static void initialize() {
        net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(entries -> {
                if (!ContentManifest.get().isBanned(ContentManifest.Category.PET_BEDS)) {
                    for (Item wood : woods()) {
                        if (PetBedWood.fromItem(wood).isEmpty()) {
                            entries.accept(stack(new Materials(BuiltInRegistries.ITEM.getKey(wood), Identifier.withDefaultNamespace("white_wool"))));
                        }
                    }
                    for (Item wool : wools()) {
                        if (!legacyWool(wool)) {
                            entries.accept(stack(new Materials(Identifier.withDefaultNamespace("oak_log"), BuiltInRegistries.ITEM.getKey(wool))));
                        }
                    }
                }
            });
    }

    public record Materials(Identifier wood, Identifier wool) {
        public static final Materials DEFAULT = new Materials(Identifier.withDefaultNamespace("oak_log"), Identifier.withDefaultNamespace("white_wool"));
        public static final Codec<Materials> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("wood").forGetter(Materials::wood),
            Identifier.CODEC.fieldOf("wool").forGetter(Materials::wool)
        ).apply(instance, Materials::new));

        public Item woodItem() {
            return BuiltInRegistries.ITEM.get(wood).map(holder -> holder.value()).filter(item -> item instanceof BlockItem).orElse(Items.OAK_LOG);
        }

        public Item woolItem() {
            return BuiltInRegistries.ITEM.get(wool).map(holder -> holder.value()).filter(item -> item instanceof BlockItem).orElse(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).asItem());
        }
    }

    public static ItemStack stack(Materials materials) {
        ItemStack stack = new ItemStack(ITEM);
        var tag = new CompoundTag();
        tag.store("materials", Materials.CODEC, materials);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(ENTITY, tag));
        return stack;
    }

    public static Materials materials(ItemStack stack) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.type() != ENTITY) {
            return Materials.DEFAULT;
        }
        return data.copyTagWithoutId().read("materials", Materials.CODEC).orElse(Materials.DEFAULT);
    }

    public static List<Item> woods() {
        return BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BlockItem)
            .filter(item -> item.builtInRegistryHolder().is(ItemTags.LOGS) || PetBedWood.fromItem(item).isPresent())
            .filter(item -> !ContentManifest.get().isBanned(item)).toList();
    }

    public static List<Item> wools() {
        return BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BlockItem)
            .filter(item -> item.builtInRegistryHolder().is(ItemTags.WOOL))
            .filter(item -> carpet(item) != Items.AIR && !ContentManifest.get().isBanned(item)).toList();
    }

    private static boolean legacyWool(Item wool) {
        return java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values())
            .anyMatch(color -> Blocks.WOOL.pick(color).asItem() == wool);
    }

    public static Item carpet(Item wool) {
        var id = BuiltInRegistries.ITEM.getKey(wool);
        if (id.getPath().endsWith("_wool")) {
            var matching = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath(id.getNamespace(),
                id.getPath().substring(0, id.getPath().length() - 5) + "_carpet"))
                .map(holder -> holder.value()).filter(item -> item.builtInRegistryHolder().is(ItemTags.WOOL_CARPETS));
            if (matching.isPresent()) {
                return matching.get();
            }
            var colors = java.util.Arrays.stream(net.minecraft.world.item.DyeColor.values())
                .sorted(java.util.Comparator.comparingInt((net.minecraft.world.item.DyeColor color) -> color.getName().length()).reversed())
                .toList();
            for (var color : colors) {
                if (id.getPath().endsWith(color.getName() + "_wool")) {
                    return Blocks.CARPET.pick(color).asItem();
                }
            }
        }
        return Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE).asItem();
    }

    public static RecipeMap addRecipes(RecipeMap original) {
        var recipes = new ArrayList<RecipeHolder<?>>(original.values());
        recipes.removeIf(recipe -> recipe.id().identifier().getNamespace().equals("granules")
            && recipe.id().identifier().getPath().startsWith("dynamic_pet_bed/"));
        if (ContentManifest.get().isBanned(ContentManifest.Category.PET_BEDS)) {
            return RecipeMap.create(recipes);
        }
        for (Item wood : woods()) {
            for (Item wool : wools()) {
                var woodId = BuiltInRegistries.ITEM.getKey(wood);
                var woolId = BuiltInRegistries.ITEM.getKey(wool);
                if (PetBedWood.fromItem(wood).isPresent() && legacyWool(wool)) {
                    continue;
                }
                var id = Identifier.fromNamespaceAndPath("granules", "dynamic_pet_bed/" + woodId.getNamespace() + "/"
                    + woodId.getPath() + "/" + woolId.getNamespace() + "/" + woolId.getPath());
                var pattern = ShapedRecipePattern.of(Map.of('T', Ingredient.of(Items.TOTEM_OF_UNDYING),
                    'L', Ingredient.of(wood), 'C', Ingredient.of(carpet(wool)), 'W', Ingredient.of(wool)), " T ", "LCL", "LWL");
                var recipe = new ShapedRecipe(new Recipe.CommonInfo(true),
                    new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.BUILDING, "granules:pet_bed"), pattern,
                    net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack(new Materials(woodId, woolId))));
                recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), recipe));
            }
        }
        return RecipeMap.create(recipes);
    }

    public static final class DynamicBlock extends PetBedBlock implements EntityBlock {
        public DynamicBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BedEntity(pos, state);
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            var entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            return List.of(stack(entity instanceof BedEntity bed ? bed.materials : Materials.DEFAULT));
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return stack(level.getBlockEntity(pos) instanceof BedEntity bed ? bed.materials : Materials.DEFAULT);
        }
    }

    public static final class BedEntity extends BlockEntity {
        private Materials materials = Materials.DEFAULT;

        public BedEntity(BlockPos pos, BlockState state) {
            super(ENTITY, pos, state);
        }

        public Materials materials() {
            return materials;
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.store("materials", Materials.CODEC, materials);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            materials = input.read("materials", Materials.CODEC).orElse(Materials.DEFAULT);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }

        @Override
        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
