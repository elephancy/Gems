package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: request to open the inscription screen for a legendary item in-hand.
 */
public record InscriptionOpenRequestPayload(boolean mainHand) implements CustomPacketPayload {
    public static final Type<InscriptionOpenRequestPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "inscription_open_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InscriptionOpenRequestPayload> CODEC = StreamCodec.of(
            InscriptionOpenRequestPayload::write,
            InscriptionOpenRequestPayload::read
    );

    public static final InscriptionOpenRequestPayload MAIN_HAND = new InscriptionOpenRequestPayload(true);
    public static final InscriptionOpenRequestPayload OFF_HAND = new InscriptionOpenRequestPayload(false);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, InscriptionOpenRequestPayload payload) {
        buf.writeBoolean(payload.mainHand());
    }

    private static InscriptionOpenRequestPayload read(RegistryFriendlyByteBuf buf) {
        return new InscriptionOpenRequestPayload(buf.readBoolean());
    }
}
