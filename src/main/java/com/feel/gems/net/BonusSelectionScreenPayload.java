package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: opens the Bonus Selection screen.
 * Contains lists of available/claimed bonus abilities and passives.
 */
public record BonusSelectionScreenPayload(
        List<BonusEntry> abilities,
        List<BonusEntry> passives,
        int maxAbilities,
        int maxPassives
) implements CustomPacketPayload {

    public record BonusEntry(
            Identifier id,
            String name,
            String description,
            boolean available,
            boolean claimed
    ) {}

    public static final Type<BonusSelectionScreenPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bonus_selection_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BonusSelectionScreenPayload> CODEC = StreamCodec.of(
            BonusSelectionScreenPayload::write,
            BonusSelectionScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, BonusSelectionScreenPayload payload) {
        // Abilities
        buf.writeVarInt(payload.abilities.size());
        for (BonusEntry entry : payload.abilities) {
            buf.writeIdentifier(entry.id());
            buf.writeUtf(entry.name(), 128);
            buf.writeUtf(entry.description(), 512);
            buf.writeBoolean(entry.available());
            buf.writeBoolean(entry.claimed());
        }

        // Passives
        buf.writeVarInt(payload.passives.size());
        for (BonusEntry entry : payload.passives) {
            buf.writeIdentifier(entry.id());
            buf.writeUtf(entry.name(), 128);
            buf.writeUtf(entry.description(), 512);
            buf.writeBoolean(entry.available());
            buf.writeBoolean(entry.claimed());
        }

        buf.writeVarInt(payload.maxAbilities);
        buf.writeVarInt(payload.maxPassives);
    }

    private static BonusSelectionScreenPayload read(RegistryFriendlyByteBuf buf) {
        // Abilities
        int abilitiesSize = buf.readVarInt();
        List<BonusEntry> abilities = new ArrayList<>(abilitiesSize);
        for (int i = 0; i < abilitiesSize; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readUtf(128);
            String description = buf.readUtf(512);
            boolean available = buf.readBoolean();
            boolean claimed = buf.readBoolean();
            abilities.add(new BonusEntry(id, name, description, available, claimed));
        }

        // Passives
        int passivesSize = buf.readVarInt();
        List<BonusEntry> passives = new ArrayList<>(passivesSize);
        for (int i = 0; i < passivesSize; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readUtf(128);
            String description = buf.readUtf(512);
            boolean available = buf.readBoolean();
            boolean claimed = buf.readBoolean();
            passives.add(new BonusEntry(id, name, description, available, claimed));
        }

        int maxAbilities = buf.readVarInt();
        int maxPassives = buf.readVarInt();

        return new BonusSelectionScreenPayload(abilities, passives, maxAbilities, maxPassives);
    }
}
