package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




public record StateSyncPayload(int activeGemOrdinal, int energy, int maxHearts) implements CustomPacketPayload {
    public static final Type<StateSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "state_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StateSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.cast(),
            StateSyncPayload::activeGemOrdinal,
            ByteBufCodecs.VAR_INT.cast(),
            StateSyncPayload::energy,
            ByteBufCodecs.VAR_INT.cast(),
            StateSyncPayload::maxHearts,
            StateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
