package com.feel.gems.power.gem.wealth;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;


public final class WealthFumble {
    private static final String KEY_UNTIL = "wealthFumbleUntil";

    private WealthFumble() {
    }

    public static void apply(ServerPlayer target, int durationTicks) {
        apply(persistent(target), GemsTime.now(target), durationTicks);
    }

    public static boolean isActive(ServerPlayer player) {
        long now = GemsTime.now(player);
        return until(persistent(player), now) > now;
    }

    public static long until(ServerPlayer player) {
        return until(persistent(player), GemsTime.now(player));
    }

    static void apply(CompoundTag nbt, long now, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        nbt.putLong(KEY_UNTIL, now + durationTicks);
    }

    static long until(CompoundTag nbt, long now) {
        long until = nbt.getLongOr(KEY_UNTIL, 0L);
        if (until <= 0) {
            return 0L;
        }
        if (until <= now) {
            nbt.remove(KEY_UNTIL);
            return 0L;
        }
        return until;
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
