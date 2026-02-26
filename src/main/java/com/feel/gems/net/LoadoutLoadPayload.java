package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload to load (apply) a saved loadout preset by index.
 */
public record LoadoutLoadPayload(GemId gem, int index) implements CustomPacketPayload {
    public static final Type<LoadoutLoadPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "loadout_load"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutLoadPayload> CODEC = StreamCodec.of(
            LoadoutLoadPayload::write,
            LoadoutLoadPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, LoadoutLoadPayload payload) {
        buf.writeUtf(payload.gem().name(), 32);
        buf.writeVarInt(payload.index());
    }

    private static LoadoutLoadPayload read(RegistryFriendlyByteBuf buf) {
        String gemName = buf.readUtf(32);
        GemId gem = GemId.valueOf(gemName);
        int index = buf.readVarInt();
        return new LoadoutLoadPayload(gem, index);
    }
}
