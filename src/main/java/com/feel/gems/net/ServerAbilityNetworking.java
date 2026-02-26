package com.feel.gems.net;

import com.feel.gems.bonus.BonusAbilityRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;




public final class ServerAbilityNetworking {
    private ServerAbilityNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.ID, (payload, context) ->
                context.server().execute(() -> GemAbilities.activateByIndex(context.player(), payload.abilityIndex())));

        ServerPlayNetworking.registerGlobalReceiver(ActivateBonusAbilityPayload.ID, (payload, context) ->
                context.server().execute(() -> BonusAbilityRuntime.activateBySlot(context.player(), payload.slotIndex())));

        ServerPlayNetworking.registerGlobalReceiver(SoulReleasePayload.ID, (payload, context) ->
                context.server().execute(() -> SoulSystem.release(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(FluxChargePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                    if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                        player.sendSystemMessage(Component.translatable("gems.flux.not_flux_gem"), true);
                        return;
                    }
                    if (FluxCharge.tryConsumeChargeItem(player)) {
                        GemStateSync.send(player);
                    }
                }));
    }
}
