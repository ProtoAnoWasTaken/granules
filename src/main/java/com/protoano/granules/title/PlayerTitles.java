package com.protoano.granules.title;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.protoano.granules.config.ContentManifest;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class PlayerTitles {
    public record Title(Identifier advancement, Component text, int color) {
        public static final Codec<Title> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("advancement").forGetter(Title::advancement),
            ComponentSerialization.CODEC.fieldOf("text").forGetter(Title::text),
            Codec.INT.fieldOf("color").forGetter(Title::color)
        ).apply(instance, Title::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Title> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Title::advancement,
            ComponentSerialization.STREAM_CODEC, Title::text,
            ByteBufCodecs.INT, Title::color, Title::new);

        public Component displayName() {
            return text.copy().withColor(color);
        }
    }

    public record Select(Identifier advancement) implements CustomPacketPayload {
        public static final Type<Select> TYPE = new Type<>(Identifier.fromNamespaceAndPath("granules", "select_title"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Select::advancement, Select::new);

        @Override
        public Type<Select> type() {
            return TYPE;
        }
    }

    public static final AttachmentType<Title> TITLE = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("granules", "player_title"), builder -> builder
            .persistent(Title.CODEC).copyOnDeath().syncWith(Title.STREAM_CODEC, AttachmentSyncPredicate.all()));

    private PlayerTitles() {
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(Select.TYPE, Select.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Select.TYPE, (payload, context) -> select(context.player(), payload.advancement()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                for (var player : server.getPlayerList().getPlayers()) {
                    validate(player);
                }
            }
        });
    }

    public static boolean select(ServerPlayer player, Identifier id) {
        if (ContentManifest.get().isBanned(ContentManifest.Category.PLAYER_TITLES)) {
            return false;
        }
        Title old = player.getAttached(TITLE);
        if (old != null && old.advancement().equals(id)) {
            player.removeAttached(TITLE);
            playSound(player, old, false);
            return true;
        }
        Title title = resolve(player, id);
        if (title == null) {
            return false;
        }
        player.setAttached(TITLE, title);
        if (old != null) {
            playSound(player, old, false);
        }
        playSound(player, title, true);
        return true;
    }

    private static void playSound(ServerPlayer player, Title title, boolean equipping) {
        var sound = switch (title.color()) {
            case 0x55FF55 -> SoundEvents.ARMOR_EQUIP_IRON;
            case 0xFF55FF -> SoundEvents.ARMOR_EQUIP_NETHERITE;
            default -> SoundEvents.ARMOR_EQUIP_LEATHER;
        };
        player.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS,
            player.getX(), player.getY(), player.getZ(), 0.6F, equipping ? 1.0F : 0.85F, player.getRandom().nextLong()));
    }

    public static void validate(ServerPlayer player) {
        Title title = player.getAttached(TITLE);
        if (title == null) {
            return;
        }
        Title valid = ContentManifest.get().isBanned(ContentManifest.Category.PLAYER_TITLES) ? null : resolve(player, title.advancement());
        if (valid == null) {
            player.removeAttached(TITLE);
        } else if (!valid.equals(title)) {
            player.setAttached(TITLE, valid);
        }
    }

    private static Title resolve(ServerPlayer player, Identifier id) {
        var advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement == null || advancement.value().display().isEmpty()
            || !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            return null;
        }
        var display = advancement.value().display().orElseThrow();
        int color = switch (display.getType()) {
            case TASK -> 0xFFFFFF;
            case GOAL -> 0x55FF55;
            case CHALLENGE -> 0xFF55FF;
        };
        return new Title(id, CosmeticTitles.resolve(id, display.getTitle()), color);
    }
}
