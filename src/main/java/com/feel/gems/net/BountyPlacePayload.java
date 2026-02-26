package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: request to place or raise a bounty on a target.
 */
public record BountyPlacePayload(UUID targetId, int hearts, int energy) implements CustomPacketPayload {
    public static final Type<BountyPlacePayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bounty_place"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BountyPlacePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.targetId());
                buf.writeVarInt(payload.hearts());
                buf.writeVarInt(payload.energy());
            },
            buf -> new BountyPlacePayload(buf.readUUID(), buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
