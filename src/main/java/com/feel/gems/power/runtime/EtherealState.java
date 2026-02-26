package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks the ethereal (damage immune) state for players.
 * When ethereal, players are immune to all damage but cannot attack.
 */
public final class EtherealState {
    private static final String KEY_ETHEREAL_UNTIL = "etherealUntil";

    private EtherealState() {
    }

    /**
     * Set the player as ethereal for the given duration in ticks.
     */
    public static void setEthereal(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            clearEthereal(player);
            return;
        }
        CompoundTag nbt = persistent(player);
        long until = GemsTime.now(player) + durationTicks;
        nbt.putLong(KEY_ETHEREAL_UNTIL, until);
    }

    /**
     * Clear the ethereal state.
     */
    public static void clearEthereal(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        nbt.remove(KEY_ETHEREAL_UNTIL);
    }

    /**
     * Check if the player is currently ethereal (immune to damage).
     */
    public static boolean isEthereal(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_ETHEREAL_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            nbt.remove(KEY_ETHEREAL_UNTIL);
            return false;
        }
        return true;
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
