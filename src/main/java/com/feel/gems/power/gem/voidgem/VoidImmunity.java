package com.feel.gems.power.gem.voidgem;

import com.feel.gems.core.GemId;
import com.feel.gems.mastery.GemMastery;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Utility for checking Void gem immunity.
 * The Void gem's only passive makes the player immune to ALL gem abilities and passives from other players.
 * No ability should ever target or affect a player with Void immunity active.
 */
public final class VoidImmunity {
    
    private VoidImmunity() {
    }

    /**
     * Check if a target player has Void immunity active.
     * Void immunity requires:
     * - Active gem is VOID
     * - Energy level >= 1 (to have passives active)
     */
    public static boolean hasImmunity(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        // Defensive: some call sites (and GameTests) may query immunity before player state is initialized.
        GemPlayerState.initIfNeeded(player);
        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.VOID) {
            return false;
        }
        return GemPowers.isPassiveActive(player, PowerIds.VOID_IMMUNITY);
    }

    /**
     * Check if an ability/passive from sourcePlayer should be blocked when targeting targetPlayer.
     * Returns true if the effect should be blocked (target has Void immunity).
     * 
     * @param sourcePlayer The player using the ability/passive (can be null for environmental effects)
     * @param targetPlayer The player being targeted
     * @return true if the effect should be blocked
     */
    public static boolean shouldBlockEffect(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) {
        if (targetPlayer == null) {
            return false;
        }
        if (sourcePlayer == null) {
            return false; // Environmental effects aren't blocked
        }
        if (sourcePlayer == targetPlayer) {
            return false; // Self-targeting abilities aren't blocked
        }
        boolean blocked = hasImmunity(targetPlayer);
        if (blocked) {
            // Track mastery progress for Void gem when blocking an effect
            GemMastery.incrementUsage(targetPlayer, GemId.VOID);
        }
        return blocked;
    }
    
    /**
     * Check if a target entity can be targeted by an ability from sourcePlayer.
     * This is the main method abilities should use for target validation.
     * 
     * Returns false (cannot be targeted) if the target is a player with Void immunity.
     * Returns true if the target can be affected by the ability.
     * 
     * @param sourcePlayer The player using the ability
     * @param target The entity being targeted (can be player or mob)
     * @return true if the target CAN be affected, false if they have Void immunity
     */
    public static boolean canBeTargeted(ServerPlayer sourcePlayer, LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (sourcePlayer == null) {
            return true; // Environmental effects can target anyone
        }
        if (target == sourcePlayer) {
            return true; // Can always target self
        }
        if (target instanceof ServerPlayer targetPlayer) {
            return !hasImmunity(targetPlayer);
        }
        return true; // Mobs can always be targeted
    }
}
