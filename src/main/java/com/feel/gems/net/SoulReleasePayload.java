package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




public enum SoulReleasePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<SoulReleasePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "soul_release"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulReleasePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

