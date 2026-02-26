package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: opens the inscription management screen for a legendary item.
 */
public record InscriptionScreenPayload(
        String itemKey,
        boolean mainHand,
        List<Entry> inscriptions,
        int maxSlots
) implements CustomPacketPayload {
    public record Entry(
            String id,
            String name,
            String description,
            String rarity,
            float magnitude
    ) {}

    public static final Type<InscriptionScreenPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "inscription_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InscriptionScreenPayload> CODEC = StreamCodec.of(
            InscriptionScreenPayload::write,
            InscriptionScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, InscriptionScreenPayload payload) {
        buf.writeUtf(payload.itemKey(), 64);
        buf.writeBoolean(payload.mainHand());
        buf.writeVarInt(payload.inscriptions().size());
        for (Entry entry : payload.inscriptions()) {
            buf.writeUtf(entry.id(), 64);
            buf.writeUtf(entry.name(), 128);
            buf.writeUtf(entry.description(), 256);
            buf.writeUtf(entry.rarity(), 16);
            buf.writeFloat(entry.magnitude());
        }
        buf.writeVarInt(payload.maxSlots());
    }

    private static InscriptionScreenPayload read(RegistryFriendlyByteBuf buf) {
        String itemKey = buf.readUtf(64);
        boolean mainHand = buf.readBoolean();
        int size = buf.readVarInt();
        List<Entry> inscriptions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(64);
            String name = buf.readUtf(128);
            String description = buf.readUtf(256);
            String rarity = buf.readUtf(16);
            float magnitude = buf.readFloat();
            inscriptions.add(new Entry(id, name, description, rarity, magnitude));
        }
        int maxSlots = buf.readVarInt();
        return new InscriptionScreenPayload(itemKey, mainHand, inscriptions, maxSlots);
    }
}
