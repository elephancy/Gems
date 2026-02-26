package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




public record ActivateAbilityPayload(int abilityIndex) implements CustomPacketPayload {
    public static final Type<ActivateAbilityPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "activate_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActivateAbilityPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT.cast(), ActivateAbilityPayload::abilityIndex, ActivateAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
