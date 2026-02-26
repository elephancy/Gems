package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> Client payload containing Prism gem ability/passive selection data.
 */
public record PrismSelectionScreenPayload(
        List<PowerEntry> gemAbilities,
        List<PowerEntry> bonusAbilities,
        List<PowerEntry> gemPassives,
        List<PowerEntry> bonusPassives,
        List<Identifier> selectedGemAbilities,
        List<Identifier> selectedBonusAbilities,
        List<Identifier> selectedGemPassives,
        List<Identifier> selectedBonusPassives,
        int maxGemAbilities,
        int maxBonusAbilities,
        int maxGemPassives,
        int maxBonusPassives
) implements CustomPacketPayload {

    /**
     * @param available True if this entry can be claimed/selected (for bonus pool entries, false if claimed by others).
     * @param claimed True if the local player currently owns/has this entry claimed (bonus pool entries only).
     */
    public record PowerEntry(Identifier id, String name, String description, String sourceName, boolean available, boolean claimed) {}

    public static final Type<PrismSelectionScreenPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "prism_selection_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrismSelectionScreenPayload> CODEC = StreamCodec.of(
            PrismSelectionScreenPayload::write,
            PrismSelectionScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, PrismSelectionScreenPayload payload) {
        writeEntryList(buf, payload.gemAbilities);
        writeEntryList(buf, payload.bonusAbilities);
        writeEntryList(buf, payload.gemPassives);
        writeEntryList(buf, payload.bonusPassives);
        writeIdList(buf, payload.selectedGemAbilities);
        writeIdList(buf, payload.selectedBonusAbilities);
        writeIdList(buf, payload.selectedGemPassives);
        writeIdList(buf, payload.selectedBonusPassives);
        buf.writeVarInt(payload.maxGemAbilities);
        buf.writeVarInt(payload.maxBonusAbilities);
        buf.writeVarInt(payload.maxGemPassives);
        buf.writeVarInt(payload.maxBonusPassives);
    }

    private static PrismSelectionScreenPayload read(RegistryFriendlyByteBuf buf) {
        List<PowerEntry> gemAbilities = readEntryList(buf);
        List<PowerEntry> bonusAbilities = readEntryList(buf);
        List<PowerEntry> gemPassives = readEntryList(buf);
        List<PowerEntry> bonusPassives = readEntryList(buf);
        List<Identifier> selectedGemAbilities = readIdList(buf);
        List<Identifier> selectedBonusAbilities = readIdList(buf);
        List<Identifier> selectedGemPassives = readIdList(buf);
        List<Identifier> selectedBonusPassives = readIdList(buf);
        int maxGemAbilities = buf.readVarInt();
        int maxBonusAbilities = buf.readVarInt();
        int maxGemPassives = buf.readVarInt();
        int maxBonusPassives = buf.readVarInt();
        return new PrismSelectionScreenPayload(
                gemAbilities, bonusAbilities, gemPassives, bonusPassives,
                selectedGemAbilities, selectedBonusAbilities, selectedGemPassives, selectedBonusPassives,
                maxGemAbilities, maxBonusAbilities, maxGemPassives, maxBonusPassives
        );
    }

    private static void writeEntryList(RegistryFriendlyByteBuf buf, List<PowerEntry> entries) {
        buf.writeVarInt(entries.size());
        for (PowerEntry entry : entries) {
            buf.writeIdentifier(entry.id);
            buf.writeUtf(entry.name);
            buf.writeUtf(entry.description);
            buf.writeUtf(entry.sourceName);
            buf.writeBoolean(entry.available);
            buf.writeBoolean(entry.claimed);
        }
    }

    private static List<PowerEntry> readEntryList(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<PowerEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readUtf();
            String description = buf.readUtf();
            String sourceName = buf.readUtf();
            boolean available = buf.readBoolean();
            boolean claimed = buf.readBoolean();
            entries.add(new PowerEntry(id, name, description, sourceName, available, claimed));
        }
        return entries;
    }

    private static void writeIdList(RegistryFriendlyByteBuf buf, List<Identifier> ids) {
        buf.writeVarInt(ids.size());
        for (Identifier id : ids) {
            buf.writeIdentifier(id);
        }
    }

    private static List<Identifier> readIdList(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Identifier> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buf.readIdentifier());
        }
        return ids;
    }
}
