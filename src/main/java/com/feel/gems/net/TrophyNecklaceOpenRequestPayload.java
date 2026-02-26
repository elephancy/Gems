package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server request to open the Trophy Necklace screen.
 */
public record TrophyNecklaceOpenRequestPayload() implements CustomPacketPayload {
    public static final TrophyNecklaceOpenRequestPayload INSTANCE = new TrophyNecklaceOpenRequestPayload();

    public static final Type<TrophyNecklaceOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "trophy_necklace_open"));

    public static final StreamCodec<FriendlyByteBuf, TrophyNecklaceOpenRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
