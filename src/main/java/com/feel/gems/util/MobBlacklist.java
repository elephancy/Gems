package com.feel.gems.util;

import com.feel.gems.config.GemsBalance;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public final class MobBlacklist {
    private static List<Identifier> cachedList = List.of();
    private static Set<Identifier> cachedSet = Set.of();

    private MobBlacklist() {
    }

    public static boolean isBlacklisted(Mob mob) {
        if (mob == null) {
            return false;
        }
        return isBlacklisted(mob.getType());
    }

    public static boolean isBlacklisted(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return isBlacklisted(id);
    }

    public static boolean isBlacklisted(Identifier id) {
        if (id == null) {
            return false;
        }
        return cachedBlacklist().contains(id);
    }

    private static Set<Identifier> cachedBlacklist() {
        List<Identifier> current = GemsBalance.v().mobBlacklist();
        if (current.equals(cachedList)) {
            return cachedSet;
        }
        cachedList = current;
        cachedSet = new HashSet<>(current);
        return cachedSet;
    }
}
