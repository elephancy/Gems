package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * S2C: informs the client that an ability started a cooldown.
 * Client uses this for HUD feedback only; server remains authoritative.
 */
public record AbilityCooldownPayload(int activeGemOrdinal, int abilityIndex, int cooldownTicks) implements CustomPacketPayload {
    public static final Type<AbilityCooldownPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "ability_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCooldownPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.cast(),
            AbilityCooldownPayload::activeGemOrdinal,
            ByteBufCodecs.VAR_INT.cast(),
            AbilityCooldownPayload::abilityIndex,
            ByteBufCodecs.VAR_INT.cast(),
            AbilityCooldownPayload::cooldownTicks,
            AbilityCooldownPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}

