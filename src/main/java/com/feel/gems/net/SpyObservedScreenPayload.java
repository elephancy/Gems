package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> Client payload containing observed Spy abilities, stolen abilities, and selections.
 */
public record SpyObservedScreenPayload(
        List<ObservedEntry> observed,
        List<StolenEntry> stolen,
        Identifier selectedEchoId,
        Identifier selectedStealId,
        Identifier selectedStolenCastId
) implements CustomPacketPayload {
    public record ObservedEntry(Identifier id, String name, int count, boolean canEcho, boolean canSteal) {}
    public record StolenEntry(Identifier id, String name) {}

    public static final Type<SpyObservedScreenPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "spy_observed_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpyObservedScreenPayload> CODEC = StreamCodec.of(
            SpyObservedScreenPayload::write,
            SpyObservedScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, SpyObservedScreenPayload payload) {
        buf.writeVarInt(payload.observed.size());
        for (ObservedEntry entry : payload.observed) {
            buf.writeIdentifier(entry.id());
            buf.writeUtf(entry.name());
            buf.writeVarInt(entry.count());
            buf.writeBoolean(entry.canEcho());
            buf.writeBoolean(entry.canSteal());
        }
        buf.writeVarInt(payload.stolen.size());
        for (StolenEntry entry : payload.stolen) {
            buf.writeIdentifier(entry.id());
            buf.writeUtf(entry.name());
        }
        writeOptional(buf, payload.selectedEchoId);
        writeOptional(buf, payload.selectedStealId);
        writeOptional(buf, payload.selectedStolenCastId);
    }

    private static SpyObservedScreenPayload read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ObservedEntry> observed = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readUtf();
            int witnessed = buf.readVarInt();
            boolean canEcho = buf.readBoolean();
            boolean canSteal = buf.readBoolean();
            observed.add(new ObservedEntry(id, name, witnessed, canEcho, canSteal));
        }
        int stolenCount = buf.readVarInt();
        List<StolenEntry> stolen = new ArrayList<>(stolenCount);
        for (int i = 0; i < stolenCount; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readUtf();
            stolen.add(new StolenEntry(id, name));
        }
        Identifier selectedEchoId = readOptional(buf);
        Identifier selectedStealId = readOptional(buf);
        Identifier selectedStolenCastId = readOptional(buf);
        return new SpyObservedScreenPayload(observed, stolen, selectedEchoId, selectedStealId, selectedStolenCastId);
    }

    private static void writeOptional(RegistryFriendlyByteBuf buf, Identifier id) {
        if (id != null) {
            buf.writeBoolean(true);
            buf.writeIdentifier(id);
        } else {
            buf.writeBoolean(false);
        }
    }

    private static Identifier readOptional(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return buf.readIdentifier();
        }
        return null;
    }
}
