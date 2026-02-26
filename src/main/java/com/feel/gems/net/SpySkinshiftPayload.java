package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


public record SpySkinshiftPayload(UUID player, Optional<UUID> target) implements CustomPacketPayload {
    public static final Type<SpySkinshiftPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "spy_skinshift"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpySkinshiftPayload> CODEC = StreamCodec.of(
            SpySkinshiftPayload::write,
            SpySkinshiftPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, SpySkinshiftPayload payload) {
        buf.writeUUID(payload.player());
        buf.writeBoolean(payload.target().isPresent());
        payload.target().ifPresent(buf::writeUUID);
    }

    private static SpySkinshiftPayload read(RegistryFriendlyByteBuf buf) {
        UUID player = buf.readUUID();
        boolean hasTarget = buf.readBoolean();
        if (!hasTarget) {
            return new SpySkinshiftPayload(player, Optional.empty());
        }
        return new SpySkinshiftPayload(player, Optional.of(buf.readUUID()));
    }
}
