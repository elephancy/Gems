package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: remove an augment from a gem item by index.
 */
public record AugmentRemovePayload(boolean mainHand, int index) implements CustomPacketPayload {
    public static final Type<AugmentRemovePayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "augment_remove"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentRemovePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.cast(), AugmentRemovePayload::mainHand,
            ByteBufCodecs.VAR_INT, AugmentRemovePayload::index,
            AugmentRemovePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
