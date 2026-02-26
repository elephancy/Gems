package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: update a Trophy Necklace passive (steal/enable/disable).
 */
public record TrophyNecklaceClaimPayload(
        Identifier passiveId,
        Action action
) implements CustomPacketPayload {

    public enum Action {
        STEAL,
        ENABLE,
        DISABLE
    }

    public static final Type<TrophyNecklaceClaimPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "trophy_necklace_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrophyNecklaceClaimPayload> CODEC = StreamCodec.of(
            TrophyNecklaceClaimPayload::write,
            TrophyNecklaceClaimPayload::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void write(RegistryFriendlyByteBuf buf, TrophyNecklaceClaimPayload payload) {
        buf.writeIdentifier(payload.passiveId);
        buf.writeVarInt(payload.action.ordinal());
    }

    private static TrophyNecklaceClaimPayload read(RegistryFriendlyByteBuf buf) {
        Identifier id = buf.readIdentifier();
        int raw = buf.readVarInt();
        Action action = Action.STEAL;
        Action[] values = Action.values();
        if (raw >= 0 && raw < values.length) {
            action = values[raw];
        }
        return new TrophyNecklaceClaimPayload(id, action);
    }
}

