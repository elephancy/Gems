package com.feel.gems.power.ability.hunter;

import com.feel.gems.state.PlayerStateManager;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class HunterPounceRuntime {
    private static final String POUNCE_TARGET_KEY = "hunter_pounce_target";
    private static final String POUNCE_DAMAGE_KEY = "hunter_pounce_damage";

    private HunterPounceRuntime() {}

    public static void setPouncing(ServerPlayer player, UUID targetId, float damage) {
        PlayerStateManager.setPersistent(player, POUNCE_TARGET_KEY, targetId.toString());
        PlayerStateManager.setPersistent(player, POUNCE_DAMAGE_KEY, String.valueOf(damage));
    }

    public static UUID getPounceTarget(ServerPlayer player) {
        String targetStr = PlayerStateManager.getPersistent(player, POUNCE_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;
        try {
            return UUID.fromString(targetStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static float getPounceDamage(ServerPlayer player) {
        String damageStr = PlayerStateManager.getPersistent(player, POUNCE_DAMAGE_KEY);
        if (damageStr == null) return 0.0F;
        return Float.parseFloat(damageStr);
    }

    public static void clearPounce(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, POUNCE_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, POUNCE_DAMAGE_KEY);
    }

    public static boolean isPouncing(ServerPlayer player) {
        return getPounceTarget(player) != null;
    }
}
