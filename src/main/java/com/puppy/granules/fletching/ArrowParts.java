package com.puppy.granules.fletching;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public record ArrowParts(Shaft shaft, Fletch fletch, Head head) {
	public static final List<ArrowParts> ALL = List.of(Shaft.values()).stream()
		.flatMap(shaft -> List.of(Fletch.values()).stream()
			.flatMap(fletch -> List.of(Head.values()).stream()
				.map(head -> new ArrowParts(shaft, fletch, head))))
		.toList();

	public String identifierSuffix() {
		return this.shaft.id + "_" + this.fletch.id + "_" + this.head.id;
	}

	public boolean isBasic() {
		return this.shaft == Shaft.STICK && this.fletch == Fletch.FEATHER && this.head == Head.FLINT;
	}

	public static Optional<ArrowParts> fromIngredients(ItemStack shaftStack, ItemStack fletchStack, ItemStack headStack) {
		for (Shaft shaft : Shaft.values()) {
			if (!shaft.matches(shaftStack)) {
				continue;
			}
			for (Fletch fletch : Fletch.values()) {
				if (!fletch.matches(fletchStack)) {
					continue;
				}
				for (Head head : Head.values()) {
					if (head.matches(headStack)) {
						return Optional.of(new ArrowParts(shaft, fletch, head));
					}
				}
			}
		}
		return Optional.empty();
	}

	public static Optional<ArrowParts> fromIdentifierSuffix(String suffix) {
		return ALL.stream().filter(parts -> parts.identifierSuffix().equals(suffix)).findFirst();
	}

	public static int inputSlotFor(ItemStack stack) {
		for (Shaft shaft : Shaft.values()) {
			if (shaft.matches(stack)) {
				return FletchingMenu.SHAFT_SLOT;
			}
		}
		for (Fletch fletch : Fletch.values()) {
			if (fletch.matches(stack)) {
				return FletchingMenu.FLETCH_SLOT;
			}
		}
		for (Head head : Head.values()) {
			if (head.matches(stack)) {
				return FletchingMenu.HEAD_SLOT;
			}
		}
		return -1;
	}

	public enum Shaft {
		STICK("stick", "minecraft:stick", "arrow_shaft_stick"),
		BONE("bone", "minecraft:bone", "arrow_shaft_bone"),
		END_ROD("end_rod", "minecraft:end_rod", "arrow_shaft_endrod"),
		BLAZE_ROD("blaze", "minecraft:blaze_rod", "arrow_shaft_blaze"),
		BREEZE_ROD("breeze", "minecraft:breeze_rod", "arrow_shaft_breeze");

		private final String id;
		private final String ingredient;
		private final String texture;

		Shaft(String id, String ingredient, String texture) {
			this.id = id;
			this.ingredient = ingredient;
			this.texture = texture;
		}

		public String ingredient() {
			return this.ingredient;
		}

		public String texture() {
			return this.texture;
		}

		private boolean matches(ItemStack stack) {
			return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(this.ingredient);
		}
	}

	public enum Fletch {
		FEATHER("feather", "minecraft:feather", "arrow_fletch_feather"),
		STRING("string", "minecraft:string", "arrow_fletch_string"),
		FIREWORK("firework", "minecraft:firework_rocket", "arrow_fletch_firework"),
		NAME_TAG("name_tag", "minecraft:name_tag", "arrow_fletch_nametag");

		private final String id;
		private final String ingredient;
		private final String texture;

		Fletch(String id, String ingredient, String texture) {
			this.id = id;
			this.ingredient = ingredient;
			this.texture = texture;
		}

		public String ingredient() {
			return this.ingredient;
		}

		public String texture() {
			return this.texture;
		}

		private boolean matches(ItemStack stack) {
			return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(this.ingredient);
		}
	}

	public enum Head {
		FLINT("flint", "minecraft:flint", "arrow_head_flint", "arrow_head_flint"),
		QUARTZ("quartz", "minecraft:quartz", "arrow_head_quartz", "arrow_quartz_head"),
		ECHO("echo", "minecraft:echo_shard", "arrow_head_echo", "arrow_head_echo"),
		AMETHYST("amethyst", "minecraft:amethyst_shard", "arrow_head_amethyst", "arrow_head_amethyst"),
		PRISMARINE("prismarine", "minecraft:prismarine_shard", "arrow_head_prismarine", "arrow_head_prismarine"),
		COAL("coal", "minecraft:coal", "arrow_head_coal", "arrow_head_coal"),
		GHAST("ghast", "minecraft:ghast_tear", "arrow_head_ghast", "arrow_head_ghast");

		private final String id;
		private final String ingredient;
		private final String itemTexture;
		private final String entityTexture;

		Head(String id, String ingredient, String itemTexture, String entityTexture) {
			this.id = id;
			this.ingredient = ingredient;
			this.itemTexture = itemTexture;
			this.entityTexture = entityTexture;
		}

		public String ingredient() {
			return this.ingredient;
		}

		public String itemTexture() {
			return this.itemTexture;
		}

		public String entityTexture() {
			return this.entityTexture;
		}

		private boolean matches(ItemStack stack) {
			return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(this.ingredient);
		}
	}
}
