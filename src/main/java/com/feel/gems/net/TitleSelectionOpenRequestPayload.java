package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: client requests opening the title selection screen.
 */
public record TitleSelectionOpenRequestPayload() implements CustomPacketPayload {
    public static final TitleSelectionOpenRequestPayload INSTANCE = new TitleSelectionOpenRequestPayload();

    public static final Type<TitleSelectionOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "title_selection_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleSelectionOpenRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
