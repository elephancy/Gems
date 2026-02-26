package com.feel.gems.util;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class GemsNbt {
    private GemsNbt() {
    }

    public static void putUuid(CompoundTag nbt, String key, UUID uuid) {
        if (uuid == null) {
            nbt.remove(key);
            return;
        }
        nbt.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
    }

    public static UUID getUuid(CompoundTag nbt, String key) {
        if (nbt == null) {
            return null;
        }
        return nbt.getIntArray(key)
                .filter(arr -> arr.length == 4)
                .map(UUIDUtil::uuidFromIntArray)
                .orElseGet(() -> nbt.getString(key).map(raw -> {
                    try {
                        return UUID.fromString(raw);
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }).orElse(null));
    }

    public static boolean containsUuid(CompoundTag nbt, String key) {
        if (nbt == null) {
            return false;
        }
        return nbt.getIntArray(key).isPresent() || nbt.getString(key).isPresent();
    }

    public static Tag fromUuid(UUID uuid) {
        if (uuid == null) {
            return StringTag.valueOf("");
        }
        return new IntArrayTag(UUIDUtil.uuidToIntArray(uuid));
    }

    public static UUID toUuid(Tag element) {
        if (element instanceof IntArrayTag array) {
            int[] raw = array.getAsIntArray();
            if (raw.length == 4) {
                return UUIDUtil.uuidFromIntArray(raw);
            }
            return null;
        }
        if (element instanceof StringTag str) {
            String raw = str.asString().orElse("");
            if (raw.isBlank()) {
                return null;
            }
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
