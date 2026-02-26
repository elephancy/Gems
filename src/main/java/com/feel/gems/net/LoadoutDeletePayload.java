package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S payload to delete a saved loadout preset by index.
 */
public record LoadoutDeletePayload(GemId gem, int index) implements CustomPacketPayload {
    public static final Type<LoadoutDeletePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "loadout_delete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutDeletePayload> CODEC = StreamCodec.of(
            LoadoutDeletePayload::write,
            LoadoutDeletePayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, LoadoutDeletePayload payload) {
        buf.writeUtf(payload.gem().name(), 32);
        buf.writeVarInt(payload.index());
    }

    private static LoadoutDeletePayload read(RegistryFriendlyByteBuf buf) {
        String gemName = buf.readUtf(32);
        GemId gem = GemId.valueOf(gemName);
        int index = buf.readVarInt();
        return new LoadoutDeletePayload(gem, index);
    }
}
