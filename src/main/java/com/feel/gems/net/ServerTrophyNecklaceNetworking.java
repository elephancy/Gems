package com.feel.gems.net;

import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.power.util.Targeting;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side networking for the Trophy Necklace passive steal UI.
 */
public final class ServerTrophyNecklaceNetworking {
    private static final ConcurrentHashMap<UUID, Set<Identifier>> OPEN_SESSIONS = new ConcurrentHashMap<>();

    private ServerTrophyNecklaceNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TrophyNecklaceOpenRequestPayload.ID, (payload, context) ->
            context.player().level().getServer().execute(() -> handleOpen(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(TrophyNecklaceClaimPayload.ID, (payload, context) ->
                context.player().level().getServer().execute(() -> handleClaim(context.player(), payload)));
    }

    public static void setSession(ServerPlayer player, Set<Identifier> offeredPassives) {
        if (player == null) {
            return;
        }
        if (offeredPassives == null || offeredPassives.isEmpty()) {
            OPEN_SESSIONS.remove(player.getUUID());
            return;
        }
        OPEN_SESSIONS.put(player.getUUID(), Set.copyOf(offeredPassives));
    }

    private static boolean isOffered(ServerPlayer player, Identifier passiveId) {
        Set<Identifier> offered = OPEN_SESSIONS.get(player.getUUID());
        return offered != null && offered.contains(passiveId);
    }

    private static void handleClaim(ServerPlayer player, TrophyNecklaceClaimPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        if (!HuntersTrophyNecklaceItem.hasNecklace(player)) {
            player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.need_necklace"), true);
            return;
        }
        Identifier passiveId = payload.passiveId();
        if (passiveId == null) {
            return;
        }
        boolean ok;
        switch (payload.action()) {
            case STEAL -> {
                if (!isOffered(player, passiveId) && !HuntersTrophyNecklaceItem.wasLastOffered(player, passiveId)) {
                    return;
                }
                UUID source = HuntersTrophyNecklaceItem.getLastTargetUuid(player);
                if (source == null) {
                    source = HuntersTrophyNecklaceItem.getLastKillTargetUuid(player);
                }
                if (source == null) {
                    player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.need_kill"), true);
                    return;
                }
                ok = HuntersTrophyNecklaceItem.stealPassiveFrom(player, passiveId, source);
            }
            case ENABLE -> ok = HuntersTrophyNecklaceItem.setPassiveEnabled(player, passiveId, true);
            case DISABLE -> ok = HuntersTrophyNecklaceItem.setPassiveEnabled(player, passiveId, false);
            default -> ok = false;
        }
        if (ok) {
            com.feel.gems.power.runtime.GemPowers.sync(player);
        }

        // Re-open the UI with the same last target, if possible.
        HuntersTrophyNecklaceItem.openLastTargetScreen(player);
    }

    private static void handleOpen(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!HuntersTrophyNecklaceItem.hasNecklace(player)) {
            player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.need_necklace"), true);
            return;
        }
        ServerPlayer target = Targeting.raycastPlayer(player, 15.0D);
        if (target != null && target != player) {
            HuntersTrophyNecklaceItem.openScreenForTarget(player, target);
            return;
        }
        boolean opened = HuntersTrophyNecklaceItem.openLastTargetScreen(player);
        if (!opened) {
            player.sendSystemMessage(Component.translatable("gems.item.trophy_necklace.no_session"), true);
        }
    }
}
