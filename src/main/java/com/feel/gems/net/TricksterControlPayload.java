package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TricksterControlPayload(boolean puppeted, boolean mindGames) implements CustomPacketPayload {
    public static final Type<TricksterControlPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "trickster_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TricksterControlPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL.cast(), TricksterControlPayload::puppeted,
                    ByteBufCodecs.BOOL.cast(), TricksterControlPayload::mindGames,
                    TricksterControlPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
