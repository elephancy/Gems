package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: client requests opening the bonus selection screen.
 */
public record BonusSelectionOpenRequestPayload() implements CustomPacketPayload {

    public static final BonusSelectionOpenRequestPayload INSTANCE = new BonusSelectionOpenRequestPayload();

    public static final Type<BonusSelectionOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bonus_selection_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BonusSelectionOpenRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
