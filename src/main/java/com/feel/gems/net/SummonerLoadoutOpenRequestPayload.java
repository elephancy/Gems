package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;




/**
 * C2S: request to open the Summoner loadout editor.
 */
public record SummonerLoadoutOpenRequestPayload() implements CustomPacketPayload {
    public static final Type<SummonerLoadoutOpenRequestPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "summoner_loadout_open_request"));
    public static final SummonerLoadoutOpenRequestPayload INSTANCE = new SummonerLoadoutOpenRequestPayload();
    public static final StreamCodec<RegistryFriendlyByteBuf, SummonerLoadoutOpenRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
