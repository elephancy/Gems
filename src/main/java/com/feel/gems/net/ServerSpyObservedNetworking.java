package com.feel.gems.net;

import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side networking for Spy observed ability selection.
 */
public final class ServerSpyObservedNetworking {
    private ServerSpyObservedNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SpyObservedOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(SpyObservedSelectPayload.ID, (payload, context) ->
                context.server().execute(() -> handleSelect(context.player(), payload)));
    }

    private static void openScreen(ServerPlayer player) {
        GemPlayerState.initIfNeeded(player);
        if (!SpySystem.isSpyActive(player)) {
            player.sendSystemMessage(Component.translatable("gems.spy.observed.not_spy"), true);
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        List<Identifier> stolen = SpySystem.getStolenAbilities(player);
        java.util.Set<Identifier> stolenSet = new java.util.HashSet<>(stolen);

        List<SpyObservedScreenPayload.ObservedEntry> entries = new ArrayList<>();
        for (SpySystem.ObservedAbility abilityInfo : SpySystem.observedAbilities(player)) {
            Identifier id = abilityInfo.id();
            if (id == null || GemsDisables.isAbilityDisabled(id)) {
                continue;
            }
            GemAbility ability = ModAbilities.get(id);
            if (ability == null) {
                continue;
            }
            boolean canEcho = SpySystem.canEcho(player, id);
            boolean canSteal = SpySystem.canSteal(player, id) && !stolenSet.contains(id);
            if (!canEcho && !canSteal) {
                continue;
            }
            entries.add(new SpyObservedScreenPayload.ObservedEntry(
                    id,
                    ability.name(),
                    abilityInfo.count(),
                    canEcho,
                    canSteal
            ));
        }

        entries.sort(Comparator.comparing(SpyObservedScreenPayload.ObservedEntry::name));

        List<SpyObservedScreenPayload.StolenEntry> stolenEntries = new ArrayList<>();
        for (Identifier id : stolen) {
            if (id == null || GemsDisables.isAbilityDisabled(id)) {
                continue;
            }
            GemAbility ability = ModAbilities.get(id);
            if (ability == null) {
                continue;
            }
            stolenEntries.add(new SpyObservedScreenPayload.StolenEntry(id, ability.name()));
        }
        stolenEntries.sort(Comparator.comparing(SpyObservedScreenPayload.StolenEntry::name));

        Identifier selectedEcho = SpySystem.selectedEchoAbility(player);
        Identifier selectedSteal = SpySystem.selectedStealAbility(player);
        Identifier selectedCast = SpySystem.selectedStolenCastAbility(player);
        ServerPlayNetworking.send(player, new SpyObservedScreenPayload(entries, stolenEntries, selectedEcho, selectedSteal, selectedCast));
    }

    private static void handleSelect(ServerPlayer player, SpyObservedSelectPayload payload) {
        if (payload == null) {
            return;
        }
        Identifier abilityId = payload.abilityId();
        if (abilityId == null) {
            return;
        }
        boolean ok = switch (payload.tab()) {
            case SpyObservedSelectPayload.TAB_ECHO -> SpySystem.selectEchoAbility(player, abilityId);
            case SpyObservedSelectPayload.TAB_STEAL -> SpySystem.selectStealAbility(player, abilityId);
            case SpyObservedSelectPayload.TAB_STOLEN_CAST -> SpySystem.selectStolenCastAbility(player, abilityId);
            default -> false;
        };
        if (!ok) {
            player.sendSystemMessage(Component.translatable("gems.spy.observed.select_failed"), true);
            return;
        }
        openScreen(player);
    }
}
