package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server request to open the Spy observed abilities screen.
 */
public record SpyObservedOpenRequestPayload() implements CustomPacketPayload {
    public static final SpyObservedOpenRequestPayload INSTANCE = new SpyObservedOpenRequestPayload();

    public static final Type<SpyObservedOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "spy_observed_open"));

    public static final StreamCodec<FriendlyByteBuf, SpyObservedOpenRequestPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
