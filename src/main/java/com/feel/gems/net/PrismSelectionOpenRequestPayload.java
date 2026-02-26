package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server request to open the Prism selection screen.
 */
public record PrismSelectionOpenRequestPayload() implements CustomPacketPayload {
    public static final PrismSelectionOpenRequestPayload INSTANCE = new PrismSelectionOpenRequestPayload();

    public static final Type<PrismSelectionOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "prism_selection_open"));

    public static final StreamCodec<FriendlyByteBuf, PrismSelectionOpenRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
