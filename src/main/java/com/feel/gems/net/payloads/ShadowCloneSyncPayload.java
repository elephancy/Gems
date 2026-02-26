package com.feel.gems.net.payloads;

import com.feel.gems.GemsMod;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C payload to sync shadow clone owner information for skin rendering.
 */
public record ShadowCloneSyncPayload(int entityId, UUID ownerUuid, String ownerName) implements CustomPacketPayload {
    
    public static final Type<ShadowCloneSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "shadow_clone_sync"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, ShadowCloneSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ShadowCloneSyncPayload::entityId,
            UUIDUtil.STREAM_CODEC, ShadowCloneSyncPayload::ownerUuid,
            ByteBufCodecs.STRING_UTF8, ShadowCloneSyncPayload::ownerName,
            ShadowCloneSyncPayload::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
