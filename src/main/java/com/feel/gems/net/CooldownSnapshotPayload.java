package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * S2C: sends a snapshot of remaining cooldown ticks and max cooldowns for the player's active gem abilities.
 */
public record CooldownSnapshotPayload(
        int activeGemOrdinal,
        java.util.List<Integer> remainingAbilityCooldownTicks,
        java.util.List<Integer> maxAbilityCooldownTicks
) implements CustomPacketPayload {
    public static final Type<CooldownSnapshotPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "cooldown_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownSnapshotPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.cast(),
            CooldownSnapshotPayload::activeGemOrdinal,
            ByteBufCodecs.collection((int size) -> (java.util.List<Integer>) new java.util.ArrayList<Integer>(size), ByteBufCodecs.VAR_INT).cast(),
            CooldownSnapshotPayload::remainingAbilityCooldownTicks,
            ByteBufCodecs.collection((int size) -> (java.util.List<Integer>) new java.util.ArrayList<Integer>(size), ByteBufCodecs.VAR_INT).cast(),
            CooldownSnapshotPayload::maxAbilityCooldownTicks,
            CooldownSnapshotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
