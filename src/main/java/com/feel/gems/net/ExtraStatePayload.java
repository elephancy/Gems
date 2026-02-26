package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * S2C: extra HUD-only state (server-authoritative but not gameplay-critical).
 */
public record ExtraStatePayload(
        int activeGemOrdinal,
        int fluxChargePercent,
        boolean hasSoul,
        String soulTypeId
) implements CustomPacketPayload {
    public static final Type<ExtraStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "extra_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtraStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.cast(),
            ExtraStatePayload::activeGemOrdinal,
            ByteBufCodecs.VAR_INT.cast(),
            ExtraStatePayload::fluxChargePercent,
            ByteBufCodecs.BOOL.cast(),
            ExtraStatePayload::hasSoul,
            ByteBufCodecs.stringUtf8(128),
            ExtraStatePayload::soulTypeId,
            ExtraStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

