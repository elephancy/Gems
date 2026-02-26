package com.feel.gems.power.ability.hunter;

import com.feel.gems.entity.HunterPackEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Runtime management for Hunter Pack entities.
 * Handles timed despawning of pack clones.
 */
public final class HunterCallThePackRuntime {
    
    private record PackInfo(UUID ownerUuid, UUID packId, int despawnAtTick) {}
    
    private static final Map<UUID, PackInfo> ACTIVE_PACKS = new ConcurrentHashMap<>();
    
    private HunterCallThePackRuntime() {}
    
    /**
     * Schedule a pack to despawn after a duration.
     */
    public static void schedulePackDespawn(UUID ownerUuid, UUID packId, int durationTicks) {
        // Store with negative despawnAtTick to indicate it needs initialization with server tick
        ACTIVE_PACKS.put(packId, new PackInfo(ownerUuid, packId, -(durationTicks)));
    }
    
    /**
     * Called every server tick to check for packs that need despawning.
     */
    public static void tick(MinecraftServer server) {
        if (ACTIVE_PACKS.isEmpty()) return;
        
        int currentTick = server.getTickCount();
        var iterator = ACTIVE_PACKS.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            PackInfo info = entry.getValue();
            
            // Initialize despawn tick on first check (negative value means duration to add)
            if (info.despawnAtTick() < 0) {
                int durationTicks = -info.despawnAtTick();
                ACTIVE_PACKS.put(entry.getKey(), new PackInfo(info.ownerUuid(), info.packId(), currentTick + durationTicks));
                continue;
            }
            
            if (currentTick >= info.despawnAtTick()) {
                despawnPack(server, info.packId());
                iterator.remove();
            }
        }
    }
    
    /**
     * Despawn all entities in a pack.
     */
    private static void despawnPack(MinecraftServer server, UUID packId) {
        for (ServerLevel world : server.getAllLevels()) {
            // Iterate all entities and check for pack members
            for (var entity : world.getAllEntities()) {
                if (entity instanceof HunterPackEntity pack && packId.equals(pack.getPackId())) {
                    pack.discard();
                }
            }
        }
    }
    
    /**
     * Called when a player disconnects to clean up their packs.
     */
    public static void onPlayerDisconnect(UUID playerUuid, MinecraftServer server) {
        var iterator = ACTIVE_PACKS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().ownerUuid().equals(playerUuid)) {
                despawnPack(server, entry.getValue().packId());
                iterator.remove();
            }
        }
    }
    
    /**
     * Clean up all packs (e.g., on server stop).
     */
    public static void clear() {
        ACTIVE_PACKS.clear();
    }
}
