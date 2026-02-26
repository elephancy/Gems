package com.feel.gems.power.ability.sentinel;

import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.trust.GemTrust;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SentinelLockdownRuntime {
    private static final Map<UUID, ZoneData> ACTIVE_ZONES = new HashMap<>();
    /** Duration of root/slowness effect applied each tick (slightly longer than tick interval for continuity) */
    private static final int ROOT_DURATION_TICKS = 25;
    /** Slowness amplifier for rooted mobs (255 = complete immobilization) */
    private static final int ROOT_SLOWNESS_AMPLIFIER = 255;
    private static final int SUPPRESS_DURATION_TICKS = 25;

    private SentinelLockdownRuntime() {}

    public static void createZone(UUID ownerId, Vec3 center, int radius, long endTime, String worldId) {
        ACTIVE_ZONES.put(ownerId, new ZoneData(ownerId, center, radius, endTime, worldId));
    }

    public static boolean isInLockdownZone(Vec3 position, UUID playerId, String worldId) {
        for (ZoneData zone : ACTIVE_ZONES.values()) {
            if (!zone.worldId.equals(worldId)) continue;
            if (zone.ownerId.equals(playerId)) continue; // Owner is immune

            double distSq = position.distanceToSqr(zone.center);
            if (distSq <= zone.radius * zone.radius) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the zone a position is in, or null if not in any zone.
     */
    public static ZoneData getZoneAt(Vec3 position, UUID excludeOwner, String worldId) {
        for (ZoneData zone : ACTIVE_ZONES.values()) {
            if (!zone.worldId.equals(worldId)) continue;
            if (excludeOwner != null && zone.ownerId.equals(excludeOwner)) continue;

            double distSq = position.distanceToSqr(zone.center);
            if (distSq <= zone.radius * zone.radius) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Tick the lockdown zones - removes expired zones and roots hostile mobs.
     */
    public static void tick(long currentTime, String worldId, ServerLevel world) {
        if (ACTIVE_ZONES.isEmpty()) {
            return;
        }
        // Remove expired zones
        ACTIVE_ZONES.entrySet().removeIf(entry ->
            entry.getValue().worldId.equals(worldId) && currentTime > entry.getValue().endTime
        );

        // Apply rooting effects to hostile mobs in active zones
        for (ZoneData zone : ACTIVE_ZONES.values()) {
            if (!zone.worldId.equals(worldId)) continue;

            ServerPlayer owner = world.getServer().getPlayerList().getPlayer(zone.ownerId);

            // Create box around zone center
            AABB zoneBox = new AABB(
                    zone.center.x - zone.radius, zone.center.y - zone.radius, zone.center.z - zone.radius,
                    zone.center.x + zone.radius, zone.center.y + zone.radius, zone.center.z + zone.radius
            );

            // Find entities in the zone
            for (Entity entity : world.getEntities(null, zoneBox)) {
                // Check if actually within radius (box is larger than sphere)
                if (entity.position().distanceToSqr(zone.center) > zone.radius * zone.radius) {
                    continue;
                }

                // Root hostile mobs
                if (entity instanceof Monster hostile) {
                    // Apply maximum slowness to root the mob in place
                    hostile.addEffect(new MobEffectInstance(
                            MobEffects.SLOWNESS,
                            ROOT_DURATION_TICKS,
                            ROOT_SLOWNESS_AMPLIFIER,
                            true, false, false));

                    // Spawn root particles occasionally
                    if (world.getRandom().nextInt(20) == 0) {
                        world.sendParticles(ParticleTypes.ENCHANTED_HIT,
                                hostile.getX(), hostile.getY() + 0.2, hostile.getZ(),
                                3, 0.3, 0.1, 0.3, 0.01);
                    }
                }

                // Apply ability restriction to enemy players (already handled elsewhere for ability blocking)
                // Just add visual feedback here
                if (entity instanceof ServerPlayer targetPlayer) {
                    if (owner != null && VoidImmunity.shouldBlockEffect(owner, targetPlayer)) {
                        continue;
                    }
                    if (owner != null && !GemTrust.isTrusted(owner, targetPlayer) && !targetPlayer.getUUID().equals(zone.ownerId)) {
                        AbilityRestrictions.suppress(targetPlayer, SUPPRESS_DURATION_TICKS);
                        // Spawn subtle particles to show they're in lockdown
                        if (world.getRandom().nextInt(40) == 0) {
                            world.sendParticles(ParticleTypes.ENCHANTED_HIT,
                                    targetPlayer.getX(), targetPlayer.getY() + 1, targetPlayer.getZ(),
                                    2, 0.2, 0.3, 0.2, 0.01);
                        }
                    }
                }
            }
        }
    }

    public static boolean hasAnyZones() {
        return !ACTIVE_ZONES.isEmpty();
    }

    public static void clearZone(UUID ownerId) {
        ACTIVE_ZONES.remove(ownerId);
    }

    public static Collection<ZoneData> getActiveZones() {
        return Collections.unmodifiableCollection(ACTIVE_ZONES.values());
    }

    public record ZoneData(UUID ownerId, Vec3 center, int radius, long endTime, String worldId) {}
}
