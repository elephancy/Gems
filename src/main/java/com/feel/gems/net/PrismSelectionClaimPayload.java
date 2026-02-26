package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server payload to claim/release a power for Prism gem.
 */
public record PrismSelectionClaimPayload(
        Identifier powerId,
        boolean isAbility,      // true = ability, false = passive
        boolean isBonus,        // true = from bonus pool, false = from normal gems
        boolean claim           // true = claim, false = release
) implements CustomPacketPayload {

    public static final Type<PrismSelectionClaimPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "prism_selection_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrismSelectionClaimPayload> CODEC = StreamCodec.of(
            PrismSelectionClaimPayload::write,
            PrismSelectionClaimPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, PrismSelectionClaimPayload payload) {
        buf.writeIdentifier(payload.powerId);
        buf.writeBoolean(payload.isAbility);
        buf.writeBoolean(payload.isBonus);
        buf.writeBoolean(payload.claim);
    }

    private static PrismSelectionClaimPayload read(RegistryFriendlyByteBuf buf) {
        Identifier powerId = buf.readIdentifier();
        boolean isAbility = buf.readBoolean();
        boolean isBonus = buf.readBoolean();
        boolean claim = buf.readBoolean();
        return new PrismSelectionClaimPayload(powerId, isAbility, isBonus, claim);
    }
}
