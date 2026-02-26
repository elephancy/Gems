package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: client requests opening the bounty board screen.
 */
public record BountyBoardOpenRequestPayload() implements CustomPacketPayload {
    public static final BountyBoardOpenRequestPayload INSTANCE = new BountyBoardOpenRequestPayload();

    public static final Type<BountyBoardOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bounty_board_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BountyBoardOpenRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
