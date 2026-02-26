package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: syncs stolen passives (Trophy Necklace) and stolen abilities (Spy).
 */
public record StolenStatePayload(List<Identifier> stolenPassives, List<Identifier> stolenAbilities) implements CustomPacketPayload {
    public static final Type<StolenStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "stolen_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StolenStatePayload> CODEC = StreamCodec.of(
            StolenStatePayload::write,
            StolenStatePayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, StolenStatePayload payload) {
        buf.writeVarInt(payload.stolenPassives().size());
        for (Identifier id : payload.stolenPassives()) {
            buf.writeUtf(id.toString(), 128);
        }
        buf.writeVarInt(payload.stolenAbilities().size());
        for (Identifier id : payload.stolenAbilities()) {
            buf.writeUtf(id.toString(), 128);
        }
    }

    private static StolenStatePayload read(RegistryFriendlyByteBuf buf) {
        int passives = buf.readVarInt();
        List<Identifier> stolenPassives = new ArrayList<>(passives);
        for (int i = 0; i < passives; i++) {
            stolenPassives.add(Identifier.parse(buf.readUtf(128)));
        }
        int abilities = buf.readVarInt();
        List<Identifier> stolenAbilities = new ArrayList<>(abilities);
        for (int i = 0; i < abilities; i++) {
            stolenAbilities.add(Identifier.parse(buf.readUtf(128)));
        }
        return new StolenStatePayload(stolenPassives, stolenAbilities);
    }
}
