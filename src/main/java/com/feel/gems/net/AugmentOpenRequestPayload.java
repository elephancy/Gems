package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: request to open the Augment management screen for a gem item in hand.
 */
public record AugmentOpenRequestPayload(boolean mainHand) implements CustomPacketPayload {
    public static final Type<AugmentOpenRequestPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "augment_open_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentOpenRequestPayload> CODEC = StreamCodec.of(
            AugmentOpenRequestPayload::write,
            AugmentOpenRequestPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, AugmentOpenRequestPayload payload) {
        buf.writeBoolean(payload.mainHand);
    }

    private static AugmentOpenRequestPayload read(RegistryFriendlyByteBuf buf) {
        return new AugmentOpenRequestPayload(buf.readBoolean());
    }
}
