package com.feel.gems.power.gem.wealth;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;


public final class HotbarLock {
    private static final String KEY_LOCK_UNTIL = "hotbarLockUntil";
    private static final String KEY_LOCK_SLOT = "hotbarLockSlot";

    private HotbarLock() {
    }

    public static void lock(ServerPlayer target, int hotbarSlot, int durationTicks) {
        lock(persistent(target), GemsTime.now(target), hotbarSlot, durationTicks);
    }

    public static boolean isLocked(ServerPlayer player) {
        return lockedSlot(player) >= 0;
    }

    public static int lockedSlot(ServerPlayer player) {
        return lockedSlot(persistent(player), GemsTime.now(player));
    }

    static void lock(CompoundTag nbt, long now, int hotbarSlot, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        int slot = clamp(hotbarSlot, 0, 8);
        nbt.putLong(KEY_LOCK_UNTIL, now + durationTicks);
        nbt.putInt(KEY_LOCK_SLOT, slot);
    }

    static int lockedSlot(CompoundTag nbt, long now) {
        long until = nbt.getLongOr(KEY_LOCK_UNTIL, 0L);
        if (until <= now) {
            if (until != 0L) {
                nbt.remove(KEY_LOCK_UNTIL);
                nbt.remove(KEY_LOCK_SLOT);
            }
            return -1;
        }
        int slot = nbt.getIntOr(KEY_LOCK_SLOT, -1);
        if (slot < 0) {
            return -1;
        }
        return clamp(slot, 0, 8);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static CompoundTag persistent(ServerPlayer player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
