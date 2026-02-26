package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: remove a legendary inscription by index from the selected hand.
 */
public record InscriptionRemovePayload(boolean mainHand, int index) implements CustomPacketPayload {
    public static final Type<InscriptionRemovePayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "inscription_remove"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, InscriptionRemovePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.cast(), InscriptionRemovePayload::mainHand,
            ByteBufCodecs.VAR_INT, InscriptionRemovePayload::index,
            InscriptionRemovePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
