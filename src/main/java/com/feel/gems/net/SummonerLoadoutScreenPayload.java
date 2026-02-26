package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * S2C: opens the Summoner loadout editor with the current loadout and summon point costs.
 */
public record SummonerLoadoutScreenPayload(
        int maxPoints,
        int maxActiveSummons,
        Map<String, Integer> costs,
        List<SummonerLoadouts.Entry> slot1,
        List<SummonerLoadouts.Entry> slot2,
        List<SummonerLoadouts.Entry> slot3,
        List<SummonerLoadouts.Entry> slot4,
        List<SummonerLoadouts.Entry> slot5
) implements CustomPacketPayload {
    public static final Type<SummonerLoadoutScreenPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "summoner_loadout_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonerLoadoutScreenPayload> CODEC = StreamCodec.of(
            SummonerLoadoutScreenPayload::write,
            SummonerLoadoutScreenPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

        private static void write(RegistryFriendlyByteBuf buf, SummonerLoadoutScreenPayload payload) {
                buf.writeVarInt(payload.maxPoints);
                buf.writeVarInt(payload.maxActiveSummons);

                Map<String, Integer> costMap = payload.costs;
                buf.writeVarInt(costMap.size());
                for (Map.Entry<String, Integer> entry : costMap.entrySet()) {
                        buf.writeUtf(entry.getKey(), 256);
                        buf.writeVarInt(entry.getValue());
                }

                writeEntries(buf, payload.slot1);
                writeEntries(buf, payload.slot2);
                writeEntries(buf, payload.slot3);
                writeEntries(buf, payload.slot4);
                writeEntries(buf, payload.slot5);
        }

        private static SummonerLoadoutScreenPayload read(RegistryFriendlyByteBuf buf) {
                int maxPoints = buf.readVarInt();
                int maxActiveSummons = buf.readVarInt();

                int costSize = buf.readVarInt();
                Map<String, Integer> costs = new HashMap<>(costSize);
                for (int i = 0; i < costSize; i++) {
                        String id = buf.readUtf(256);
                        int cost = buf.readVarInt();
                        costs.put(id, cost);
                }

                List<SummonerLoadouts.Entry> s1 = readEntries(buf);
                List<SummonerLoadouts.Entry> s2 = readEntries(buf);
                List<SummonerLoadouts.Entry> s3 = readEntries(buf);
                List<SummonerLoadouts.Entry> s4 = readEntries(buf);
                List<SummonerLoadouts.Entry> s5 = readEntries(buf);

                return new SummonerLoadoutScreenPayload(maxPoints, maxActiveSummons, costs, s1, s2, s3, s4, s5);
        }

        private static void writeEntries(RegistryFriendlyByteBuf buf, List<SummonerLoadouts.Entry> entries) {
                buf.writeVarInt(entries.size());
                for (SummonerLoadouts.Entry entry : entries) {
                        buf.writeUtf(entry.entityId(), 256);
                        buf.writeVarInt(entry.count());
                }
        }

        private static List<SummonerLoadouts.Entry> readEntries(RegistryFriendlyByteBuf buf) {
                int size = buf.readVarInt();
                List<SummonerLoadouts.Entry> out = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                        String id = buf.readUtf(256);
                        int count = buf.readVarInt();
                        out.add(new SummonerLoadouts.Entry(id, count));
                }
                return out;
        }
}
