package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Syncs rivalry target information to the client for HUD display.
 */
public record RivalrySyncPayload(String targetName) implements CustomPacketPayload {
    public static final Type<RivalrySyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "rivalry_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RivalrySyncPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8.cast(), RivalrySyncPayload::targetName, RivalrySyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
