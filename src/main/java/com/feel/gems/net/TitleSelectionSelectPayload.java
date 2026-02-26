package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: player selects a title to display (or clears it with an empty id).
 */
public record TitleSelectionSelectPayload(String titleId) implements CustomPacketPayload {
    public static final Type<TitleSelectionSelectPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "title_selection_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleSelectionSelectPayload> CODEC = StreamCodec.of(
            TitleSelectionSelectPayload::write,
            TitleSelectionSelectPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, TitleSelectionSelectPayload payload) {
        String raw = payload.titleId == null ? "" : payload.titleId;
        buf.writeUtf(raw, 128);
    }

    private static TitleSelectionSelectPayload read(RegistryFriendlyByteBuf buf) {
        return new TitleSelectionSelectPayload(buf.readUtf(128));
    }
}
