package com.feel.gems.power.gem.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * Duelist passive runtime implementations.
 */
public final class DuelistPassiveRuntime {
    private static final String RIPOSTE_END_KEY = "duelist_riposte_end";

    private DuelistPassiveRuntime() {}

    // ========== Riposte ==========
    // After a successful block (parry), your next attack within 2s deals 50% bonus damage.

    public static void triggerRiposte(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.DUELIST_RIPOSTE)) {
            return;
        }
        int windowTicks = GemsBalance.v().duelist().riposteWindowTicks();
        long endTime = player.level().getGameTime() + windowTicks;
        PlayerStateManager.setPersistent(player, RIPOSTE_END_KEY, String.valueOf(endTime));
    }

    public static boolean consumeRiposte(ServerPlayer player) {
        String endStr = PlayerStateManager.getPersistent(player, RIPOSTE_END_KEY);
        if (endStr == null || endStr.isEmpty()) return false;

        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearRiposte(player);
            return false;
        }

        clearRiposte(player);
        return true;
    }

    public static void clearRiposte(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, RIPOSTE_END_KEY);
    }

    public static float getRiposteDamageMultiplier() {
        return GemsBalance.v().duelist().riposteBonusDamageMultiplier();
    }

    // ========== Duelist's Focus ==========
    // Deal 25% more damage in 1v1 combat when no other players are within 15 blocks.

    public static boolean isIn1v1Combat(ServerPlayer attacker, ServerPlayer target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.DUELIST_FOCUS)) {
            return false;
        }

        int range = GemsBalance.v().duelist().focusRadiusBlocks();
        AABB searchBox = attacker.getBoundingBox().inflate(range);
        if (!(attacker.level() instanceof ServerLevel world)) {
            return false;
        }

        List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(
            ServerPlayer.class,
            searchBox,
            p -> p != attacker && p != target && p.isAlive()
        );

        return nearbyPlayers.isEmpty();
    }

    public static float getFocusDamageMultiplier() {
        return GemsBalance.v().duelist().focusBonusDamageMultiplier();
    }

    // ========== Combat Stance ==========
    // While holding a sword, gain 10% movement speed.
    // This is handled via status effect application in the passive tick.
}
