package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * C2S: request to consume 1 charge item for the Flux gem.
 */
public enum FluxChargePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<FluxChargePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "flux_charge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluxChargePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

