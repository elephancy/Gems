package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class TricksterMindGamesRuntime {
    private static final String MIND_GAMES_END_KEY = "trickster_mind_games_end";
    
    // Command tag prefixes for mob mind games (similar to HypnoControl pattern)
    private static final String TAG_CONFUSED = "gems_confused";
    private static final String TAG_CONFUSED_UNTIL_PREFIX = "gems_confused_until:";

    private TricksterMindGamesRuntime() {}

    // ========== Player Mind Games ==========

    public static void applyMindGames(ServerPlayer player, int durationTicks) {
        long endTime = player.level().getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(player, MIND_GAMES_END_KEY, String.valueOf(endTime));
        TricksterControlSync.sync(player);
    }

    public static boolean hasReversedControls(ServerPlayer player) {
        String endStr = PlayerStateManager.getPersistent(player, MIND_GAMES_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.level().getGameTime() > endTime) {
            clearMindGames(player);
            return false;
        }

        return true;
    }

    public static void clearMindGames(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, MIND_GAMES_END_KEY);
        TricksterControlSync.sync(player);
    }

    // ========== Mob Mind Games ==========

    /**
     * Apply mind games effect to a mob using command tags, making it confused and attack random targets.
     */
    public static void applyMindGamesMob(Mob mob, int durationTicks) {
        long endTime = mob.level().getGameTime() + durationTicks;
        
        // Clear any existing confused tags first
        clearConfusedMobTags(mob);
        
        // Add new confused tags
        mob.addTag(TAG_CONFUSED);
        mob.addTag(TAG_CONFUSED_UNTIL_PREFIX + endTime);
    }

    /**
     * Check if a mob is currently under mind games effect.
     */
    public static boolean isMobConfused(Mob mob) {
        if (!mob.entityTags().contains(TAG_CONFUSED)) {
            return false;
        }
        
        long until = getMobConfusedUntil(mob);
        if (until > 0 && mob.level().getGameTime() > until) {
            clearMindGamesMob(mob);
            return false;
        }
        
        return true;
    }

    /**
     * Get the end tick for a confused mob.
     */
    private static long getMobConfusedUntil(Mob mob) {
        for (String tag : mob.entityTags()) {
            if (tag.startsWith(TAG_CONFUSED_UNTIL_PREFIX)) {
                try {
                    return Long.parseLong(tag.substring(TAG_CONFUSED_UNTIL_PREFIX.length()));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    /**
     * Tick confused mob to maintain random hostile behavior.
     * Called periodically to switch the mob's target to random nearby entities.
     */
    public static void tickConfusedMob(Mob mob, ServerLevel world) {
        if (!isMobConfused(mob)) {
            return;
        }

        // Switch targets frequently enough to be noticeable (mobs often retarget immediately).
        LivingEntity currentTarget = mob.getTarget();
        boolean shouldReroll = currentTarget == null || !currentTarget.isAlive() || world.getRandom().nextFloat() < 0.35f;
        if (!shouldReroll) {
            return;
        }

        AABB searchBox = mob.getBoundingBox().inflate(16);
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != mob && e.isAlive()
        );

        if (!nearbyEntities.isEmpty()) {
            LivingEntity randomTarget = nearbyEntities.get(world.getRandom().nextInt(nearbyEntities.size()));
            mob.setTarget(randomTarget);
        }
    }

    public static void clearMindGamesMob(Mob mob) {
        clearConfusedMobTags(mob);
    }

    private static void clearConfusedMobTags(Mob mob) {
        mob.removeTag(TAG_CONFUSED);
        for (String tag : java.util.List.copyOf(mob.entityTags())) {
            if (tag.startsWith(TAG_CONFUSED_UNTIL_PREFIX)) {
                mob.removeTag(tag);
            }
        }
    }
}
