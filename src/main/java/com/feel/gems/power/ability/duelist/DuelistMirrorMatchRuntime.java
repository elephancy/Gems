package com.feel.gems.power.ability.duelist;

import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.state.PlayerStateManager;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.net.GemStateSync;

public final class DuelistMirrorMatchRuntime {
    private static final String LOGOUT_PENALTY_KEY = "duelist_mirror_match_logout_penalty";

    private DuelistMirrorMatchRuntime() {
    }

    public static void start(ServerPlayer caster, ServerPlayer target) {
        if (caster == null || target == null) {
            return;
        }
        // Swap appearances for the duration.
        syncDisguise(caster, target.getUUID());
        syncDisguise(target, caster.getUUID());
    }

    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!DuelistMirrorMatchAbility.isInDuel(player)) {
            String partnerStr = PlayerStateManager.getPersistent(player, DuelistMirrorMatchAbility.DUEL_PARTNER_KEY);
            if (partnerStr != null && !partnerStr.isEmpty()) {
                clear(player, false);
            }
        }
    }

    /**
     * Clear duel state and optionally teleport the player back to spawn.
     * @param teleportBack whether to teleport the player back to their saved spawn position
     */
    public static void clear(ServerPlayer player, boolean teleportBack) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        UUID partner = partnerUuid(player);

        // Remove the barrier cage before clearing state
        removeCageIfPresent(player);

        DuelistMirrorMatchAbility.clearDuel(player);
        syncDisguise(player, null);

        // Teleport winner back to spawn
        if (teleportBack && server != null) {
            teleportToRespawn(server, player);
        }

        // Ensure the partner's disguise is also cleared for all viewers, even if the partner is offline.
        if (server != null && partner != null) {
            ServerPlayer partnerPlayer = server.getPlayerList().getPlayer(partner);
            if (partnerPlayer != null) {
                // Note: We don't call removeCageIfPresent here because both players share the same cage
                // and we already removed it above. Also don't teleport partner - they lost/died.
                DuelistMirrorMatchAbility.clearDuel(partnerPlayer);
                syncDisguise(partnerPlayer, null);
            } else {
                clearDisguiseForAllViewers(server, partner);
            }
        }
    }

    /**
     * Called when a player dies during a duel - loser doesn't get teleported back.
     */
    public static void onDeath(ServerPlayer player) {
        if (!DuelistMirrorMatchAbility.isInDuel(player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        UUID partner = partnerUuid(player);
        removeCageIfPresent(player);

        DuelistMirrorMatchAbility.clearDuel(player);
        syncDisguise(player, null);

        if (server != null && partner != null) {
            ServerPlayer partnerPlayer = server.getPlayerList().getPlayer(partner);
            if (partnerPlayer != null) {
                DuelistMirrorMatchAbility.clearDuel(partnerPlayer);
                syncDisguise(partnerPlayer, null);
                teleportToRespawn(server, partnerPlayer);
            } else {
                clearDisguiseForAllViewers(server, partner);
            }
        }
    }

    /**
     * Called when a player disconnects during a duel.
     * Both players should be teleported back and the cage removed.
     */
    public static void onDisconnect(ServerPlayer player, MinecraftServer server) {
        if (!DuelistMirrorMatchAbility.isInDuel(player)) {
            return;
        }
        
        UUID partnerUuid = partnerUuid(player);
        BlockPos cageCenter = DuelistMirrorMatchAbility.getCageCenter(player);
        
        // Remove cage before clearing state
        if (cageCenter != null) {
            for (ServerLevel world : server.getAllLevels()) {
                DuelistMirrorMatchAbility.removeCage(world, cageCenter);
            }
        }
        
        // Clear the disconnecting player (they can't be teleported since they're disconnecting)
        DuelistMirrorMatchAbility.clearDuel(player);
        markLogoutPenalty(player);
        syncDisguise(player, null);
        clearDisguiseForAllViewers(server, player.getUUID());
        
        // Teleport partner back to their saved spawn
        if (partnerUuid != null) {
            ServerPlayer partner = server.getPlayerList().getPlayer(partnerUuid);
            if (partner != null) {
                teleportToRespawn(server, partner);
                DuelistMirrorMatchAbility.clearDuel(partner);
                syncDisguise(partner, null);
            } else {
                clearDisguiseForAllViewers(server, partnerUuid);
            }
        }
    }

    /**
     * Remove the barrier cage if the player is in a duel with a cage.
     */
    private static void removeCageIfPresent(ServerPlayer player) {
        BlockPos cageCenter = DuelistMirrorMatchAbility.getCageCenter(player);
        if (cageCenter != null && player.level() instanceof ServerLevel world) {
            DuelistMirrorMatchAbility.removeCage(world, cageCenter);
        }
    }

    private static void syncDisguise(ServerPlayer player, java.util.UUID targetUuid) {
        if (player.level().getServer() == null) {
            return;
        }
        SpySkinshiftPayload payload = new SpySkinshiftPayload(player.getUUID(), Optional.ofNullable(targetUuid));
        for (ServerPlayer viewer : player.level().getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static UUID partnerUuid(ServerPlayer player) {
        String partnerStr = PlayerStateManager.getPersistent(player, DuelistMirrorMatchAbility.DUEL_PARTNER_KEY);
        if (partnerStr == null || partnerStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(partnerStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void clearDisguiseForAllViewers(MinecraftServer server, UUID playerId) {
        SpySkinshiftPayload payload = new SpySkinshiftPayload(playerId, Optional.empty());
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    public static void applyLogoutPenalty(ServerPlayer player) {
        String flag = PlayerStateManager.getPersistent(player, LOGOUT_PENALTY_KEY);
        if (!"true".equals(flag)) {
            return;
        }
        PlayerStateManager.clearPersistent(player, LOGOUT_PENALTY_KEY);
        GemPlayerState.addEnergy(player, -2);
        GemPlayerState.addMaxHearts(player, -2);
        GemPlayerState.applyMaxHearts(player);
        GemStateSync.send(player);
    }

    private static void markLogoutPenalty(ServerPlayer player) {
        PlayerStateManager.setPersistent(player, LOGOUT_PENALTY_KEY, "true");
    }

    private static void teleportToRespawn(MinecraftServer server, ServerPlayer player) {
        var respawn = player.getRespawnConfig();
        var spawnPoint = respawn == null ? null : respawn.respawnData();
        ResourceKey<Level> respawnKey = spawnPoint == null ? null : spawnPoint.dimension();
        BlockPos respawnPos = spawnPoint == null ? null : spawnPoint.pos();
        if (respawnKey == null || respawnPos == null) {
            ServerLevel overworld = server.overworld();
            respawnKey = Level.OVERWORLD;
            respawnPos = overworld.getRespawnData().pos();
        }
        ServerLevel world = server.getLevel(respawnKey);
        if (world == null) {
            world = server.overworld();
        }
        player.teleportTo(world, respawnPos.getX() + 0.5, respawnPos.getY(), respawnPos.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);
    }
}
