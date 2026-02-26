package com.feel.gems.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;




/**
 * Small helpers for persistent per-player NBT.
 */
public final class PlayerNbt {
    private PlayerNbt() {
    }

    public static boolean getBoolean(ServerPlayer player, String key, boolean fallback) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return nbt.getBoolean(key).orElse(fallback);
    }

    public static void putBoolean(ServerPlayer player, String key, boolean value) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putBoolean(key, value);
    }

    public static int getInt(ServerPlayer player, String key, int fallback) {
        CompoundTag nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return nbt.getInt(key).orElse(fallback);
    }

    public static void putInt(ServerPlayer player, String key, int value) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putInt(key, value);
    }
}

