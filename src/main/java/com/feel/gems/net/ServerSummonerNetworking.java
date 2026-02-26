package com.feel.gems.net;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.power.gem.summoner.SummonerLoadouts;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;




public final class ServerSummonerNetworking {
    private ServerSummonerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SummonerLoadoutSavePayload.ID, (payload, context) ->
                context.player().level().getServer().execute(() -> handleSave(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(SummonerLoadoutOpenRequestPayload.ID, (payload, context) ->
            context.player().level().getServer().execute(() -> openEditor(context.player())));
    }

    public static void openEditor(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getActiveGem(player) != GemId.SUMMONER) {
            player.sendSystemMessage(Component.translatable("gems.summoner.switch_to_edit"), true);
            return;
        }

        GemsBalance.Summoner cfg = GemsBalance.v().summoner();
        SummonerLoadouts.Loadout loadout = SummonerLoadouts.resolve(player, cfg);

        ServerPlayNetworking.send(player, new SummonerLoadoutScreenPayload(
                cfg.maxPoints(),
                cfg.maxActiveSummons(),
                cfg.costs(),
                loadout.slot1(),
                loadout.slot2(),
                loadout.slot3(),
                loadout.slot4(),
                loadout.slot5()
        ));
    }

    private static void handleSave(ServerPlayer player, SummonerLoadoutSavePayload payload) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getActiveGem(player) != GemId.SUMMONER) {
            player.sendSystemMessage(Component.translatable("gems.summoner.must_use_summoner"), true);
            return;
        }

        GemsBalance.Summoner cfg = GemsBalance.v().summoner();
        SummonerLoadouts.Loadout requested = SummonerLoadouts.fromEntries(
                payload.slot1(),
                payload.slot2(),
                payload.slot3(),
                payload.slot4(),
                payload.slot5()
        );
        SummonerLoadouts.Loadout sanitized = SummonerLoadouts.sanitize(requested, cfg);
        SummonerLoadouts.save(player, sanitized);
        player.sendSystemMessage(Component.translatable("gems.summoner.loadout_saved"), true);
    }
}
