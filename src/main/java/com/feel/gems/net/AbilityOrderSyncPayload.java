package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: syncs the ordered ability list for the active gem.
 */
public record AbilityOrderSyncPayload(int activeGemOrdinal, List<Identifier> abilityOrder) implements CustomPacketPayload {
    public static final Type<AbilityOrderSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "ability_order_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityOrderSyncPayload> CODEC = StreamCodec.of(
            AbilityOrderSyncPayload::write,
            AbilityOrderSyncPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, AbilityOrderSyncPayload payload) {
        buf.writeVarInt(payload.activeGemOrdinal());
        buf.writeVarInt(payload.abilityOrder().size());
        for (Identifier id : payload.abilityOrder()) {
            buf.writeUtf(id.toString(), 128);
        }
    }

    private static AbilityOrderSyncPayload read(RegistryFriendlyByteBuf buf) {
        int gemOrdinal = buf.readVarInt();
        int count = buf.readVarInt();
        List<Identifier> order = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            order.add(Identifier.parse(buf.readUtf(128)));
        }
        return new AbilityOrderSyncPayload(gemOrdinal, order);
    }
}
