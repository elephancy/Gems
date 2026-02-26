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
 * S2C: opens the bounty board screen with current bounties and placement data.
 */
public record BountyBoardPayload(
        UUID viewerId,
        int maxAdditionalHearts,
        int maxAdditionalEnergy,
        List<Entry> bounties,
        List<PlayerEntry> players
) implements CustomPacketPayload {
    public record Entry(
            UUID targetId,
            String targetName,
            UUID placerId,
            String placerName,
            int hearts,
            int energy
    ) {}

    public record PlayerEntry(UUID uuid, String name) {}

    public static final Type<BountyBoardPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bounty_board"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BountyBoardPayload> CODEC = StreamCodec.of(
            BountyBoardPayload::write,
            BountyBoardPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, BountyBoardPayload payload) {
        buf.writeUUID(payload.viewerId());
        buf.writeVarInt(payload.maxAdditionalHearts());
        buf.writeVarInt(payload.maxAdditionalEnergy());

        List<Entry> entries = payload.bounties();
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUUID(entry.targetId());
            buf.writeUtf(entry.targetName(), 128);
            buf.writeUUID(entry.placerId());
            buf.writeUtf(entry.placerName(), 128);
            buf.writeVarInt(entry.hearts());
            buf.writeVarInt(entry.energy());
        }

        List<PlayerEntry> players = payload.players();
        buf.writeVarInt(players.size());
        for (PlayerEntry player : players) {
            buf.writeUUID(player.uuid());
            buf.writeUtf(player.name(), 128);
        }
    }

    private static BountyBoardPayload read(RegistryFriendlyByteBuf buf) {
        UUID viewer = buf.readUUID();
        int maxHearts = buf.readVarInt();
        int maxEnergy = buf.readVarInt();

        int entryCount = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            UUID targetId = buf.readUUID();
            String targetName = buf.readUtf(128);
            UUID placerId = buf.readUUID();
            String placerName = buf.readUtf(128);
            int hearts = buf.readVarInt();
            int energy = buf.readVarInt();
            entries.add(new Entry(targetId, targetName, placerId, placerName, hearts, energy));
        }

        int playerCount = buf.readVarInt();
        List<PlayerEntry> players = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf(128);
            players.add(new PlayerEntry(uuid, name));
        }

        return new BountyBoardPayload(viewer, maxHearts, maxEnergy, entries, players);
    }
}
