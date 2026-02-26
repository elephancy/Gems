package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: request to open the loadout presets manager for a gem.
 */
public record LoadoutOpenRequestPayload(GemId gem) implements CustomPacketPayload {
    public static final Type<LoadoutOpenRequestPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "loadout_open_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoadoutOpenRequestPayload> CODEC = StreamCodec.of(
            LoadoutOpenRequestPayload::write,
            LoadoutOpenRequestPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, LoadoutOpenRequestPayload payload) {
        buf.writeUtf(payload.gem().name(), 32);
    }

    private static LoadoutOpenRequestPayload read(RegistryFriendlyByteBuf buf) {
        String gemName = buf.readUtf(32);
        return new LoadoutOpenRequestPayload(GemId.valueOf(gemName));
    }
}
