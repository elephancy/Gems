package com.feel.gems.trust;

import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.feel.gems.util.GemsNbt;




public final class GemTrust {
    private static final String KEY_TRUSTED = "trustedPlayers";
    private static final String KEY_TRUST_VERSION = "trustedPlayersVersion";

    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private GemTrust() {
    }

    public static boolean isTrusted(ServerPlayer owner, ServerPlayer other) {
        if (owner == other) {
            return true;
        }
        return getTrusted(owner).contains(other.getUUID());
    }

    public static Set<UUID> getTrusted(Player owner) {
        CompoundTag root = root(owner);
        int version = root.getIntOr(KEY_TRUST_VERSION, 0);
        UUID ownerId = owner.getUUID();
        CacheEntry cached = CACHE.get(ownerId);
        if (cached != null && cached.version == version) {
            return cached.trusted;
        }

        ListTag list = root.getList(KEY_TRUSTED).orElse(null);
        if (list == null) {
            Set<UUID> empty = Set.of();
            CACHE.put(ownerId, new CacheEntry(version, empty));
            return empty;
        }
        Set<UUID> result = new HashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid != null) {
                result.add(uuid);
            }
        }
        Set<UUID> frozen = Set.copyOf(result);
        CACHE.put(ownerId, new CacheEntry(version, frozen));
        return frozen;
    }

    public static boolean trust(Player owner, UUID uuid) {
        Set<UUID> current = new HashSet<>(getTrusted(owner));
        boolean changed = current.add(uuid);
        if (changed) {
            writeTrusted(owner, current);
        }
        return changed;
    }

    public static boolean untrust(Player owner, UUID uuid) {
        Set<UUID> current = new HashSet<>(getTrusted(owner));
        boolean changed = current.remove(uuid);
        if (changed) {
            writeTrusted(owner, current);
        }
        return changed;
    }

    private static void writeTrusted(Player owner, Set<UUID> uuids) {
        ListTag list = new ListTag();
        for (UUID uuid : uuids) {
            list.add(GemsNbt.fromUuid(uuid));
        }
        CompoundTag root = root(owner);
        root.put(KEY_TRUSTED, list);
        int nextVersion = root.getIntOr(KEY_TRUST_VERSION, 0) + 1;
        root.putInt(KEY_TRUST_VERSION, nextVersion);
        CACHE.put(owner.getUUID(), new CacheEntry(nextVersion, Set.copyOf(uuids)));
    }

    private static CompoundTag root(Player player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    public static void clearRuntimeCache(UUID ownerUuid) {
        CACHE.remove(ownerUuid);
    }

    private static final class CacheEntry {
        final int version;
        final Set<UUID> trusted;

        private CacheEntry(int version, Set<UUID> trusted) {
            this.version = version;
            this.trusted = trusted;
        }
    }
}
