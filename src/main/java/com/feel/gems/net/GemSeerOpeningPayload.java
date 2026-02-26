package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.screen.GemSeerScreenHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Sends the Gem Seer player list after vanilla opens its menu. */
public record GemSeerOpeningPayload(int menuId, GemSeerScreenHandler.OpeningData data) implements CustomPacketPayload {
    public static final Type<GemSeerOpeningPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gem_seer_opening"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GemSeerOpeningPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.menuId());
                GemSeerScreenHandler.OpeningData.PACKET_CODEC.encode(buf, payload.data());
            },
            buf -> new GemSeerOpeningPayload(
                    buf.readVarInt(),
                    GemSeerScreenHandler.OpeningData.PACKET_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
