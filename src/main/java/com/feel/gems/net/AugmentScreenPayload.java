package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: opens the Augment management screen for a gem.
 */
public record AugmentScreenPayload(
        String gemId,
        boolean mainHand,
        List<AugmentEntry> augments,
        int maxSlots
) implements CustomPacketPayload {
    public record AugmentEntry(
            String id,
            String name,
            String description,
            String rarity,
            float magnitude
    ) {}

    public static final Type<AugmentScreenPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "augment_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AugmentScreenPayload> CODEC = StreamCodec.of(
            AugmentScreenPayload::write,
            AugmentScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, AugmentScreenPayload payload) {
        buf.writeUtf(payload.gemId(), 32);
        buf.writeBoolean(payload.mainHand());
        buf.writeVarInt(payload.augments().size());
        for (AugmentEntry entry : payload.augments()) {
            buf.writeUtf(entry.id(), 64);
            buf.writeUtf(entry.name(), 128);
            buf.writeUtf(entry.description(), 256);
            buf.writeUtf(entry.rarity(), 16);
            buf.writeFloat(entry.magnitude());
        }
        buf.writeVarInt(payload.maxSlots());
    }

    private static AugmentScreenPayload read(RegistryFriendlyByteBuf buf) {
        String gemId = buf.readUtf(32);
        boolean mainHand = buf.readBoolean();
        int size = buf.readVarInt();
        List<AugmentEntry> augments = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(64);
            String name = buf.readUtf(128);
            String description = buf.readUtf(256);
            String rarity = buf.readUtf(16);
            float magnitude = buf.readFloat();
            augments.add(new AugmentEntry(id, name, description, rarity, magnitude));
        }
        int maxSlots = buf.readVarInt();
        return new AugmentScreenPayload(gemId, mainHand, augments, maxSlots);
    }
}
