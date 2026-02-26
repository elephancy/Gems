package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * C2S: player submits a Summoner loadout edit.
 */
public record SummonerLoadoutSavePayload(
        List<SummonerLoadouts.Entry> slot1,
        List<SummonerLoadouts.Entry> slot2,
        List<SummonerLoadouts.Entry> slot3,
        List<SummonerLoadouts.Entry> slot4,
        List<SummonerLoadouts.Entry> slot5
) implements CustomPacketPayload {
    public static final Type<SummonerLoadoutSavePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "summoner_loadout_save"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonerLoadoutSavePayload> CODEC = StreamCodec.of(
            SummonerLoadoutSavePayload::write,
            SummonerLoadoutSavePayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

        private static void write(RegistryFriendlyByteBuf buf, SummonerLoadoutSavePayload payload) {
                writeEntries(buf, payload.slot1);
                writeEntries(buf, payload.slot2);
                writeEntries(buf, payload.slot3);
                writeEntries(buf, payload.slot4);
                writeEntries(buf, payload.slot5);
        }

        private static SummonerLoadoutSavePayload read(RegistryFriendlyByteBuf buf) {
                List<SummonerLoadouts.Entry> s1 = readEntries(buf);
                List<SummonerLoadouts.Entry> s2 = readEntries(buf);
                List<SummonerLoadouts.Entry> s3 = readEntries(buf);
                List<SummonerLoadouts.Entry> s4 = readEntries(buf);
                List<SummonerLoadouts.Entry> s5 = readEntries(buf);
                return new SummonerLoadoutSavePayload(s1, s2, s3, s4, s5);
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