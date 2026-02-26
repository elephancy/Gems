package com.feel.gems.net;

import com.feel.gems.loadout.GemLoadout;
import com.feel.gems.loadout.LoadoutManager;
import com.feel.gems.loadout.GemLoadout.HudLayout;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;




public final class ServerClientConfigNetworking {
    private ServerClientConfigNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ClientPassiveTogglePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                    GemPlayerState.setPassivesEnabled(player, payload.enabled());
                    GemStateSync.send(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(LoadoutSavePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                GemLoadout current = LoadoutManager.createFromCurrent(player, payload.name());
                List<net.minecraft.resources.Identifier> abilities = payload.abilityOrder().isEmpty()
                    ? current.abilityOrder()
                    : payload.abilityOrder();
                List<net.minecraft.resources.Identifier> available = com.feel.gems.core.GemRegistry.definition(current.gem()).abilities();
                abilities = GemLoadout.sanitizeAbilityOrder(abilities, available);
                GemLoadout.HudLayout hud = new GemLoadout.HudLayout(
                    payload.hudPosition(),
                    payload.showCooldowns(),
                    payload.showEnergy(),
                    payload.compactMode()
                );
                GemLoadout loadout = new GemLoadout(
                    GemLoadout.sanitizeName(payload.name()),
                    current.gem(),
                    abilities,
                    payload.passivesEnabled(),
                    hud
                );
                    int index = LoadoutManager.savePreset(player, loadout);
                    if (index >= 0) {
                        LoadoutManager.applyLoadout(player, loadout);
                        LoadoutManager.setActivePresetIndex(player, loadout.gem(), index);
                        GemStateSync.send(player);
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(LoadoutLoadPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                    LoadoutManager.loadPreset(player, payload.gem(), payload.index());
                    GemStateSync.send(player);
                }));

        ServerPlayNetworking.registerGlobalReceiver(LoadoutDeletePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                    LoadoutManager.deletePreset(player, payload.gem(), payload.index());
                }));

        ServerPlayNetworking.registerGlobalReceiver(LoadoutOpenRequestPayload.ID, (payload, context) ->
            context.server().execute(() -> {
                var player = context.player();
                GemPlayerState.initIfNeeded(player);
                if (!LoadoutManager.canUseLoadouts(player)) {
                player.sendSystemMessage(
                    Component.translatable("gems.loadout.locked", GemsBalance.v().loadouts().unlockEnergy())
                        .withStyle(ChatFormatting.RED),
                    true
                );
                return;
                }

                GemId gem = payload.gem();
                List<GemLoadout> presets = LoadoutManager.getPresets(player, gem);
                List<LoadoutScreenPayload.Preset> entries = new ArrayList<>(presets.size());
                for (GemLoadout loadout : presets) {
                HudLayout hud = loadout.hudLayout();
                entries.add(new LoadoutScreenPayload.Preset(
                    loadout.name(),
                    loadout.passivesEnabled(),
                    hud.position(),
                    hud.showCooldowns(),
                    hud.showEnergy(),
                    hud.compactMode()
                ));
                }

                int active = LoadoutManager.getActivePresetIndex(player, gem);
                int unlock = GemsBalance.v().loadouts().unlockEnergy();
                int maxPresets = GemsBalance.v().loadouts().maxPresetsPerGem();
                List<net.minecraft.resources.Identifier> abilityOrder = LoadoutManager.getAbilityOrder(player, gem);
                ServerPlayNetworking.send(player, new LoadoutScreenPayload(gem, unlock, maxPresets, active, abilityOrder, entries));
            }));
    }
}
