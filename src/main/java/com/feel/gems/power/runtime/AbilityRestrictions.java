package com.feel.gems.power.runtime;

import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;




public final class AbilityRestrictions {
    private static final String KEY_SUPPRESSED_UNTIL = "abilitySuppressedUntil";
    private static final String KEY_STUNNED_UNTIL = "abilityStunnedUntil";

    private AbilityRestrictions() {
    }

    public static boolean isSuppressed(ServerPlayer player) {
        long now = GemsTime.now(player);
        return suppressedUntil(player) > now;
    }

    public static boolean isStunned(ServerPlayer player) {
        long now = GemsTime.now(player);
        return stunnedUntil(player) > now;
    }

    public static void suppress(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        if (VoidImmunity.hasImmunity(player)) {
            return;
        }
        persistent(player).putLong(KEY_SUPPRESSED_UNTIL, GemsTime.now(player) + durationTicks);
        GemPowers.sync(player);
    }

    public static void stun(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        if (VoidImmunity.hasImmunity(player)) {
            return;
        }
        persistent(player).putLong(KEY_STUNNED_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static long suppressedUntil(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_SUPPRESSED_UNTIL, 0L);
        long now = GemsTime.now(player);
        if (until > 0 && until <= now) {
            nbt.remove(KEY_SUPPRESSED_UNTIL);
            return 0L;
        }
        return until;
    }

    public static long stunnedUntil(ServerPlayer player) {
        CompoundTag nbt = persistent(player);
        long until = nbt.getLongOr(KEY_STUNNED_UNTIL, 0L);
        long now = GemsTime.now(player);
        if (until > 0 && until <= now) {
            nbt.remove(KEY_STUNNED_UNTIL);
            return 0L;
        }
        return until;
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
