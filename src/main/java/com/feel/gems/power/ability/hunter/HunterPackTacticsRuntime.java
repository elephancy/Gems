package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.PlayerStateManager;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class HunterPackTacticsRuntime {
    private static final String PACK_TARGET_KEY = "hunter_pack_tactics_target";
    private static final String PACK_END_KEY = "hunter_pack_tactics_end";

    private HunterPackTacticsRuntime() {}

    public static void grantBuff(ServerPlayer player, UUID targetId, int durationTicks) {
        long endTime = player.level().getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(player, PACK_TARGET_KEY, targetId.toString());
        PlayerStateManager.setPersistent(player, PACK_END_KEY, String.valueOf(endTime));
    }

    public static boolean hasBuffAgainst(ServerPlayer attacker, UUID targetId) {
        String storedTarget = PlayerStateManager.getPersistent(attacker, PACK_TARGET_KEY);
        if (storedTarget == null || !storedTarget.equals(targetId.toString())) {
            return false;
        }

        String endStr = PlayerStateManager.getPersistent(attacker, PACK_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (attacker.level().getGameTime() > endTime) {
            clearBuff(attacker);
            return false;
        }

        return true;
    }

    public static float getDamageMultiplier() {
        return GemsBalance.v().hunter().packTacticsBonusDamageMultiplier();
    }

    public static void clearBuff(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, PACK_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, PACK_END_KEY);
    }
}
