package com.puppy.granules.item;

import com.puppy.granules.GranulesMod;
import com.puppy.granules.block.PetBedBlock;
import com.puppy.granules.block.PetBedWood;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class PetBedItem extends BlockItem {
	private static final String WOOD_KEY = "PetBedWood";
	private static final String WOOL_KEY = "PetBedWool";

	public PetBedItem(PetBedBlock block, Properties properties) {
		super(block, properties);
	}

	@Override
	protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
		BlockState placementState = super.getPlacementState(context);
		if (placementState == null) {
			return null;
		}
		ItemStack stack = context.getItemInHand();
		return placementState
			.setValue(PetBedBlock.WOOD, woodOf(stack))
			.setValue(PetBedBlock.WOOL, woolOf(stack));
	}

	public static ItemStack createStack(PetBedWood wood, DyeColor wool) {
		ItemStack stack = new ItemStack(GranulesMod.PET_BED_ITEM);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putString(WOOD_KEY, wood.getSerializedName());
			tag.putString(WOOL_KEY, wool.getName());
		});
		stack.set(
			DataComponents.CUSTOM_MODEL_DATA,
			new CustomModelData(List.of(), List.of(), List.of(wood.getSerializedName() + "_" + wool.getName()), List.of())
		);
		return stack;
	}

	public static PetBedWood woodOf(ItemStack stack) {
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		String name = customData.copyTag().getStringOr(WOOD_KEY, PetBedWood.OAK.getSerializedName());
		for (PetBedWood wood : PetBedWood.values()) {
			if (wood.getSerializedName().equals(name)) {
				return wood;
			}
		}
		return PetBedWood.OAK;
	}

	public static DyeColor woolOf(ItemStack stack) {
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		return DyeColor.byName(customData.copyTag().getStringOr(WOOL_KEY, DyeColor.WHITE.getName()), DyeColor.WHITE);
	}
}
