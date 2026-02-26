package com.feel.gems.admin;

import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.PlayerNbt;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small persistent admin toggles (per-player).
 */
public final class GemsAdmin {
    private static final String KEY_NO_COOLDOWNS = "gemsAdminNoCooldowns";
    private static final String KEY_NO_LEGENDARY_COOLDOWNS = "gemsAdminNoLegendaryCooldowns";

    private GemsAdmin() {
    }

    public static boolean noCooldowns(ServerPlayer player) {
        return PlayerNbt.getBoolean(player, KEY_NO_COOLDOWNS, false);
    }

    public static void setNoCooldowns(ServerPlayer player, boolean enabled) {
        PlayerNbt.putBoolean(player, KEY_NO_COOLDOWNS, enabled);
        if (enabled) {
            GemAbilityCooldowns.clearAll(player);
        }
    }

    public static boolean noLegendaryCooldowns(ServerPlayer player) {
        return PlayerNbt.getBoolean(player, KEY_NO_LEGENDARY_COOLDOWNS, false);
    }

    public static void setNoLegendaryCooldowns(ServerPlayer player, boolean enabled) {
        PlayerNbt.putBoolean(player, KEY_NO_LEGENDARY_COOLDOWNS, enabled);
    }
}

