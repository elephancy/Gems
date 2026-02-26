package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class TricksterPuppetRuntime {
    private static final String PUPPETED_BY_KEY = "trickster_puppeted_by";
    private static final String PUPPETED_END_KEY = "trickster_puppeted_end";
    
    // Command tag prefixes for mob puppeting (similar to HypnoControl pattern)
    private static final String TAG_PUPPETED = "gems_puppeted";
    private static final String TAG_PUPPETED_BY_PREFIX = "gems_puppeted_by:";
    private static final String TAG_PUPPETED_UNTIL_PREFIX = "gems_puppeted_until:";

    private TricksterPuppetRuntime() {}

    // ========== Player Puppeting ==========

    public static void setPuppeted(ServerPlayer target, UUID puppeteerId, int durationTicks) {
        long endTime = target.level().getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(target, PUPPETED_BY_KEY, puppeteerId.toString());
        PlayerStateManager.setPersistent(target, PUPPETED_END_KEY, String.valueOf(endTime));
        TricksterControlSync.sync(target);
    }

    public static UUID getPuppeteer(ServerPlayer player) {
        String puppeteerStr = PlayerStateManager.getPersistent(player, PUPPETED_BY_KEY);
        if (puppeteerStr == null || puppeteerStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(player, PUPPETED_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearPuppeted(player);
            return null;
        }

        try {
            return UUID.fromString(puppeteerStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isPuppeted(ServerPlayer player) {
        return getPuppeteer(player) != null;
    }

    public static void clearPuppeted(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, PUPPETED_BY_KEY);
        PlayerStateManager.clearPersistent(player, PUPPETED_END_KEY);
        TricksterControlSync.sync(player);
    }

    public static void tickPuppetedPlayer(ServerPlayer target) {
        UUID puppeteerId = getPuppeteer(target);
        if (puppeteerId == null) {
            return;
        }
        ServerLevel world = target.level();
        if (world == null || world.getServer() == null) {
            return;
        }
        ServerPlayer puppeteer = world.getServer().getPlayerList().getPlayer(puppeteerId);
        if (puppeteer == null) {
            clearPuppeted(target);
            return;
        }

        // Mirror the puppeteer's movement and facing.
        target.setDeltaMovement(puppeteer.getDeltaMovement());
        target.needsSync = true;
        target.setYRot(puppeteer.getYRot());
        target.setXRot(puppeteer.getXRot());
        target.setYHeadRot(puppeteer.getYHeadRot());
    }

    // ========== Mob Puppeting ==========

    /**
     * Mark a mob as puppeted using command tags. The mob will attack its allies for the duration.
     */
    public static void setPuppetedMob(Mob mob, UUID puppeteerId, int durationTicks) {
        long endTime = mob.level().getGameTime() + durationTicks;
        
        // Clear any existing puppeted tags first
        clearPuppetedMobTags(mob);
        
        // Add new puppeted tags
        mob.addTag(TAG_PUPPETED);
        mob.addTag(TAG_PUPPETED_BY_PREFIX + puppeteerId.toString());
        mob.addTag(TAG_PUPPETED_UNTIL_PREFIX + endTime);
    }

    /**
     * Check if a mob is currently puppeted.
     */
    public static boolean isMobPuppeted(Mob mob) {
        if (!mob.entityTags().contains(TAG_PUPPETED)) {
            return false;
        }
        
        long until = getMobPuppetedUntil(mob);
        if (until > 0 && mob.level().getGameTime() > until) {
            clearPuppetedMob(mob);
            return false;
        }
        
        return true;
    }

    /**
     * Get the end tick for a puppeted mob.
     */
    private static long getMobPuppetedUntil(Mob mob) {
        for (String tag : mob.entityTags()) {
            if (tag.startsWith(TAG_PUPPETED_UNTIL_PREFIX)) {
                try {
                    return Long.parseLong(tag.substring(TAG_PUPPETED_UNTIL_PREFIX.length()));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    /**
     * Tick puppeted mob to maintain hostile behavior towards allies.
     */
    public static void tickPuppetedMob(Mob mob, ServerLevel world) {
        if (!isMobPuppeted(mob)) {
            return;
        }

        // Prefer targeting other mobs so the effect is visible and persists against vanilla AI retargeting.
        LivingEntity currentTarget = mob.getTarget();
        boolean needsNewTarget = currentTarget == null || !currentTarget.isAlive() || !(currentTarget instanceof Mob);
        if (!needsNewTarget) {
            return;
        }

        AABB searchBox = mob.getBoundingBox().inflate(16);
        List<Mob> nearbyMobs = world.getEntitiesOfClass(
                Mob.class,
                searchBox,
                e -> e != mob && e.isAlive()
        );

        if (!nearbyMobs.isEmpty()) {
            Mob newTarget = nearbyMobs.get(world.getRandom().nextInt(nearbyMobs.size()));
            mob.setTarget(newTarget);
            return;
        }

        // Fallback: if there are no other mobs around, pick any living entity so the effect still does something.
        List<LivingEntity> nearbyLiving = world.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != mob && e.isAlive()
        );
        if (!nearbyLiving.isEmpty()) {
            mob.setTarget(nearbyLiving.get(world.getRandom().nextInt(nearbyLiving.size())));
        }
    }

    public static void clearPuppetedMob(Mob mob) {
        clearPuppetedMobTags(mob);
        mob.setTarget(null);
    }

    private static void clearPuppetedMobTags(Mob mob) {
        mob.removeTag(TAG_PUPPETED);
        // Remove any puppeted_by and puppeted_until tags (avoid mutating the tag set directly).
        for (String tag : List.copyOf(mob.entityTags())) {
            if (tag.startsWith(TAG_PUPPETED_BY_PREFIX) || tag.startsWith(TAG_PUPPETED_UNTIL_PREFIX)) {
                mob.removeTag(tag);
            }
        }
    }
}
