package com.feel.gems.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for managing temporary and persistent player state.
 * 
 * Persistent state survives logout/death (stored in player NBT).
 * Temporary state is in-memory and cleared on logout (tick-based countdowns).
 */
public final class PlayerStateManager {
    private static final String GEMS_STATE_KEY = "GemsCustomState";
    
    // In-memory temporary state: player UUID -> key -> remaining ticks
    private static final Map<UUID, Map<String, Integer>> TEMPORARY_STATE = new ConcurrentHashMap<>();

    private PlayerStateManager() {
    }

    // ======================= PERSISTENT STATE =======================
    // Stored in player NBT, survives logout/death

    public static void setPersistent(ServerPlayer player, String key, String value) {
        CompoundTag root = getOrCreateRoot(player);
        root.putString(key, value);
        persistRoot(player, root);
    }

    public static String getPersistent(ServerPlayer player, String key) {
        CompoundTag root = getOrCreateRoot(player);
        if (!root.contains(key)) {
            return null;
        }
        return root.getStringOr(key, "");
    }

    public static void clearPersistent(ServerPlayer player, String key) {
        CompoundTag root = getOrCreateRoot(player);
        root.remove(key);
        persistRoot(player, root);
    }

    public static boolean hasPersistent(ServerPlayer player, String key) {
        return getPersistent(player, key) != null;
    }

    private static CompoundTag getOrCreateRoot(ServerPlayer player) {
        CompoundTag playerData = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag root = playerData.getCompound(GEMS_STATE_KEY).orElse(null);
        if (root == null) {
            root = new CompoundTag();
            playerData.put(GEMS_STATE_KEY, root);
        }
        return root;
    }

    private static void persistRoot(ServerPlayer player, CompoundTag root) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().put(GEMS_STATE_KEY, root);
    }

    // ======================= TEMPORARY STATE =======================
    // In-memory only, cleared on logout, tick-based countdowns

    public static void setTemporary(ServerPlayer player, String key, int ticks) {
        TEMPORARY_STATE.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).put(key, ticks);
    }

    public static int getTemporary(ServerPlayer player, String key) {
        Map<String, Integer> playerState = TEMPORARY_STATE.get(player.getUUID());
        if (playerState == null) {
            return 0;
        }
        return playerState.getOrDefault(key, 0);
    }

    public static void clearTemporary(ServerPlayer player, String key) {
        Map<String, Integer> playerState = TEMPORARY_STATE.get(player.getUUID());
        if (playerState != null) {
            playerState.remove(key);
        }
    }

    public static boolean hasTemporary(ServerPlayer player, String key) {
        return getTemporary(player, key) > 0;
    }

    /**
     * Called each server tick to decrement temporary state counters.
     */
    public static void tickAll() {
        for (Map.Entry<UUID, Map<String, Integer>> entry : TEMPORARY_STATE.entrySet()) {
            Map<String, Integer> playerState = entry.getValue();
            playerState.entrySet().removeIf(e -> {
                int remaining = e.getValue() - 1;
                if (remaining <= 0) {
                    return true;
                }
                e.setValue(remaining);
                return false;
            });
        }
    }

    /**
     * Called when a player logs out to clean up their temporary state.
     */
    public static void onPlayerLogout(ServerPlayer player) {
        TEMPORARY_STATE.remove(player.getUUID());
    }

    /**
     * Called when a player dies to optionally clear temporary state.
     */
    public static void onPlayerDeath(ServerPlayer player) {
        // Clear temporary state on death
        TEMPORARY_STATE.remove(player.getUUID());
    }
}
