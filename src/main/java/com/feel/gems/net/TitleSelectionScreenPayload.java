package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: opens the title selection screen with progress data.
 */
public record TitleSelectionScreenPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(
            String id,
            int gemOrdinal,
            String displayKey,
            int usage,
            int threshold,
            boolean unlocked,
            boolean selected,
            boolean forcedSelected
    ) {
    }

    public static final Type<TitleSelectionScreenPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "title_selection_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleSelectionScreenPayload> CODEC = StreamCodec.of(
            TitleSelectionScreenPayload::write,
            TitleSelectionScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, TitleSelectionScreenPayload payload) {
        List<Entry> entries = payload.entries();
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUtf(entry.id(), 128);
            buf.writeVarInt(entry.gemOrdinal());
            buf.writeUtf(entry.displayKey(), 128);
            buf.writeVarInt(entry.usage());
            buf.writeVarInt(entry.threshold());
            buf.writeBoolean(entry.unlocked());
            buf.writeBoolean(entry.selected());
            buf.writeBoolean(entry.forcedSelected());
        }
    }

    private static TitleSelectionScreenPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(128);
            int gemOrdinal = buf.readVarInt();
            String displayKey = buf.readUtf(128);
            int usage = buf.readVarInt();
            int threshold = buf.readVarInt();
            boolean unlocked = buf.readBoolean();
            boolean selected = buf.readBoolean();
            boolean forcedSelected = buf.readBoolean();
            entries.add(new Entry(id, gemOrdinal, displayKey, usage, threshold, unlocked, selected, forcedSelected));
        }
        return new TitleSelectionScreenPayload(entries);
    }
}
