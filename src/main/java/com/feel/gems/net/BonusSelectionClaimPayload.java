package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: claim or unclaim a bonus ability/passive.
 */
public record BonusSelectionClaimPayload(
        Identifier powerId,
        boolean isAbility,
        boolean claim // true = claim, false = unclaim
) implements CustomPacketPayload {

    public static final Type<BonusSelectionClaimPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "bonus_selection_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BonusSelectionClaimPayload> CODEC = StreamCodec.of(
            BonusSelectionClaimPayload::write,
            BonusSelectionClaimPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, BonusSelectionClaimPayload payload) {
        buf.writeIdentifier(payload.powerId);
        buf.writeBoolean(payload.isAbility);
        buf.writeBoolean(payload.claim);
    }

    private static BonusSelectionClaimPayload read(RegistryFriendlyByteBuf buf) {
        Identifier powerId = buf.readIdentifier();
        boolean isAbility = buf.readBoolean();
        boolean claim = buf.readBoolean();
        return new BonusSelectionClaimPayload(powerId, isAbility, claim);
    }
}
