package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: player selects a Tracker Compass target (or clears it).
 */
public record TrackerCompassSelectPayload(Optional<UUID> target) implements CustomPacketPayload {
    public static final Type<TrackerCompassSelectPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "tracker_compass_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackerCompassSelectPayload> CODEC = StreamCodec.of(
            TrackerCompassSelectPayload::write,
            TrackerCompassSelectPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, TrackerCompassSelectPayload payload) {
        Optional<UUID> target = payload.target;
        buf.writeBoolean(target.isPresent());
        if (target.isPresent()) {
            buf.writeUUID(target.get());
        }
    }

    private static TrackerCompassSelectPayload read(RegistryFriendlyByteBuf buf) {
        boolean present = buf.readBoolean();
        if (!present) {
            return new TrackerCompassSelectPayload(Optional.empty());
        }
        return new TrackerCompassSelectPayload(Optional.of(buf.readUUID()));
    }
}
