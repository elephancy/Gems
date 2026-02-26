package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player ability disables (used by Spy theft).
 *
 * <p>Stored as persistent player NBT and checked on every ability activation.</p>
 */
public final class AbilityDisables {
    private static final String KEY_DISABLED = "gemsDisabledAbilities";

    private AbilityDisables() {
    }

    public static boolean isDisabled(ServerPlayer player, Identifier abilityId) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        CompoundTag set = root.getCompound(KEY_DISABLED).orElse(null);
        return set != null && set.contains(abilityId.toString());
    }

    public static boolean disable(ServerPlayer player, Identifier abilityId) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String raw = abilityId.toString();
        CompoundTag set = root.getCompound(KEY_DISABLED).orElseGet(CompoundTag::new);
        if (set.contains(raw)) {
            return false;
        }
        set.putBoolean(raw, true);
        root.put(KEY_DISABLED, set);
        return true;
    }

    public static boolean enable(ServerPlayer player, Identifier abilityId) {
        CompoundTag root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String needle = abilityId.toString();
        CompoundTag set = root.getCompound(KEY_DISABLED).orElse(null);
        if (set == null) {
            return false;
        }
        boolean removed = set.remove(needle) != null;
        if (set.isEmpty()) {
            root.remove(KEY_DISABLED);
        } else {
            root.put(KEY_DISABLED, set);
        }
        return removed;
    }
}
