package com.feel.gems.power.ability.sentinel;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public final class SentinelInterventionRuntime {
    private static final String PROTECTING_KEY = "sentinel_intervention_protecting";
    private static final String PROTECTED_BY_KEY = "sentinel_intervention_protected_by";

    private SentinelInterventionRuntime() {}

    public static void setProtecting(ServerPlayer sentinel, ServerPlayer ally) {
        if (sentinel == null || ally == null) {
            return;
        }
        if (sentinel == ally) {
            return;
        }
        MinecraftServer server = sentinel.level().getServer();
        if (server == null) {
            return;
        }

        // Clear any previous protection the sentinel had.
        UUID prevAlly = getProtectedAlly(sentinel);
        if (prevAlly != null) {
            ServerPlayer prev = server.getPlayerList().getPlayer(prevAlly);
            if (prev != null) {
                clearProtectedBy(prev, sentinel.getUUID());
            }
        }

        // Clear any existing protector on the ally.
        UUID prevProtector = getProtectorUuid(ally);
        if (prevProtector != null && !prevProtector.equals(sentinel.getUUID())) {
            ServerPlayer prevSentinel = server.getPlayerList().getPlayer(prevProtector);
            if (prevSentinel != null) {
                clearProtecting(prevSentinel, ally.getUUID());
            }
        }

        PlayerStateManager.setPersistent(sentinel, PROTECTING_KEY, ally.getStringUUID());
        PlayerStateManager.setPersistent(ally, PROTECTED_BY_KEY, sentinel.getStringUUID());
    }

    public static UUID getProtectedAlly(ServerPlayer sentinel) {
        String allyStr = PlayerStateManager.getPersistent(sentinel, PROTECTING_KEY);
        if (allyStr == null || allyStr.isEmpty()) return null;
        try {
            return UUID.fromString(allyStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isProtecting(ServerPlayer sentinel) {
        return getProtectedAlly(sentinel) != null;
    }

    public static void consumeProtection(ServerPlayer sentinel) {
        if (sentinel == null) {
            return;
        }
        MinecraftServer server = sentinel.level().getServer();
        UUID ally = getProtectedAlly(sentinel);
        PlayerStateManager.clearPersistent(sentinel, PROTECTING_KEY);
        if (server != null && ally != null) {
            ServerPlayer allyPlayer = server.getPlayerList().getPlayer(ally);
            if (allyPlayer != null) {
                clearProtectedBy(allyPlayer, sentinel.getUUID());
            }
        }
    }

    /**
     * Called when the protected ally would take damage.
     * Returns the sentinel who should take the damage instead, or null if no protection.
     */
    public static ServerPlayer getProtector(ServerPlayer ally) {
        if (ally == null) {
            return null;
        }
        MinecraftServer server = ally.level().getServer();
        if (server == null) {
            return null;
        }
        UUID protectorId = getProtectorUuid(ally);
        if (protectorId == null) {
            return null;
        }
        ServerPlayer sentinel = server.getPlayerList().getPlayer(protectorId);
        if (sentinel == null) {
            PlayerStateManager.clearPersistent(ally, PROTECTED_BY_KEY);
            return null;
        }
        UUID protectedId = getProtectedAlly(sentinel);
        if (protectedId == null || !protectedId.equals(ally.getUUID())) {
            PlayerStateManager.clearPersistent(ally, PROTECTED_BY_KEY);
            return null;
        }
        return sentinel;
    }

    public static void clearProtection(ServerPlayer sentinel) {
        consumeProtection(sentinel);
    }

    public static void cleanup(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) {
            return;
        }

        UUID ally = getProtectedAlly(player);
        if (ally != null) {
            ServerPlayer allyPlayer = server.getPlayerList().getPlayer(ally);
            PlayerStateManager.clearPersistent(player, PROTECTING_KEY);
            if (allyPlayer != null) {
                clearProtectedBy(allyPlayer, player.getUUID());
            }
        }

        UUID protector = getProtectorUuid(player);
        if (protector != null) {
            ServerPlayer sentinel = server.getPlayerList().getPlayer(protector);
            PlayerStateManager.clearPersistent(player, PROTECTED_BY_KEY);
            if (sentinel != null) {
                clearProtecting(sentinel, player.getUUID());
            }
        }
    }

    private static UUID getProtectorUuid(ServerPlayer ally) {
        String uuidStr = PlayerStateManager.getPersistent(ally, PROTECTED_BY_KEY);
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void clearProtecting(ServerPlayer sentinel, UUID allyId) {
        UUID current = getProtectedAlly(sentinel);
        if (current != null && current.equals(allyId)) {
            PlayerStateManager.clearPersistent(sentinel, PROTECTING_KEY);
        }
    }

    private static void clearProtectedBy(ServerPlayer ally, UUID sentinelId) {
        UUID current = getProtectorUuid(ally);
        if (current != null && current.equals(sentinelId)) {
            PlayerStateManager.clearPersistent(ally, PROTECTED_BY_KEY);
        }
    }
}
