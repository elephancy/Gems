package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;




public final class GemAbilityCooldowns {
    private static final String KEY_COOLDOWNS = "cooldowns";

    private GemAbilityCooldowns() {
    }

    public static void clearAll(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_COOLDOWNS);
    }

    public static long nextAllowedTick(ServerPlayer player, Identifier abilityId) {
        return cooldowns(player).getLongOr(abilityId.toString(), 0L);
    }

    public static void setNextAllowedTick(ServerPlayer player, Identifier abilityId, long nextAllowedTick) {
        cooldowns(player).putLong(abilityId.toString(), nextAllowedTick);
    }

    public static int remainingTicks(ServerPlayer player, Identifier abilityId, long nowTick) {
        long nextAllowed = nextAllowedTick(player, abilityId);
        if (nextAllowed <= nowTick) {
            return 0;
        }
        long remaining = nextAllowed - nowTick;
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    private static CompoundTag cooldowns(ServerPlayer player) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag existing = root.getCompound(KEY_COOLDOWNS).orElse(null);
        if (existing == null) {
            existing = new CompoundTag();
            root.put(KEY_COOLDOWNS, existing);
        }
        return existing;
    }
}

