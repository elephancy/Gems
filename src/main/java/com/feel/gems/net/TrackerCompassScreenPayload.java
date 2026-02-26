package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: opens the Tracker Compass target picker.
 */
public record TrackerCompassScreenPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(UUID uuid, String name, boolean online) {
    }

    public static final Type<TrackerCompassScreenPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "tracker_compass_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrackerCompassScreenPayload> CODEC = StreamCodec.of(
            TrackerCompassScreenPayload::write,
            TrackerCompassScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, TrackerCompassScreenPayload payload) {
        List<Entry> entries = payload.entries;
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUUID(entry.uuid());
            buf.writeUtf(entry.name(), 128);
            buf.writeBoolean(entry.online());
        }
    }

    private static TrackerCompassScreenPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(128);
            boolean online = buf.readBoolean();
            entries.add(new Entry(uuid, name, online));
        }
        return new TrackerCompassScreenPayload(entries);
    }
}
