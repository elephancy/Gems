package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server payload to select an observed ability for Spy echo/steal, or a stolen cast target.
 */
public record SpyObservedSelectPayload(int tab, Identifier abilityId) implements CustomPacketPayload {
    public static final Type<SpyObservedSelectPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "spy_observed_select"));

    public static final int TAB_ECHO = 0;
    public static final int TAB_STEAL = 1;
    public static final int TAB_STOLEN_CAST = 2;

    public static final StreamCodec<RegistryFriendlyByteBuf, SpyObservedSelectPayload> CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT.cast(),
            SpyObservedSelectPayload::tab,
            Identifier.STREAM_CODEC,
            SpyObservedSelectPayload::abilityId,
            SpyObservedSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
