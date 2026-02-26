package com.feel.gems.net;

import com.feel.gems.core.GemId;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;




public final class GemExtraStateSync {
    private GemExtraStateSync() {
    }

    public static void send(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);

        int flux = FluxCharge.get(player);
        boolean hasSoul = SoulSystem.hasSoul(player);
        String soulType = hasSoul ? SoulSystem.soulType(player) : "";

        ServerPlayNetworking.send(player, new ExtraStatePayload(active.ordinal(), flux, hasSoul, soulType));
    }
}

