package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




public record ClientPassiveTogglePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<ClientPassiveTogglePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "client_passive_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientPassiveTogglePayload> CODEC = StreamCodec.of(
            ClientPassiveTogglePayload::write,
            ClientPassiveTogglePayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, ClientPassiveTogglePayload payload) {
        buf.writeBoolean(payload.enabled());
    }

    private static ClientPassiveTogglePayload read(RegistryFriendlyByteBuf buf) {
        return new ClientPassiveTogglePayload(buf.readBoolean());
    }
}
