package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> Server: Activate a claimed bonus ability by index (0 or 1).
 */
public record ActivateBonusAbilityPayload(int slotIndex) implements CustomPacketPayload {
    public static final Type<ActivateBonusAbilityPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "activate_bonus_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateBonusAbilityPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT.cast(), ActivateBonusAbilityPayload::slotIndex, ActivateBonusAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
