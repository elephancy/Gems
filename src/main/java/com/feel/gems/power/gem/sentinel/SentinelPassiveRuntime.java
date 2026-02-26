package com.feel.gems.power.gem.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/**
 * Sentinel passive runtime implementations.
 */
public final class SentinelPassiveRuntime {
    private static final String FORTRESS_POS_KEY = "sentinel_fortress_pos";
    private static final String FORTRESS_START_KEY = "sentinel_fortress_start";

    private static final Map<UUID, GuardianAuraCacheEntry> GUARDIAN_AURA_CACHE = new HashMap<>();

    private SentinelPassiveRuntime() {}

    // ========== Guardian Aura ==========
    // Nearby trusted allies take 15% less damage.

    public static boolean isProtectedByGuardianAura(ServerPlayer victim) {
        if (VoidImmunity.hasImmunity(victim)) {
            return false;
        }
        if (!(victim.level() instanceof ServerLevel world)) {
            return false;
        }
        long tick = world.getGameTime();
        GuardianAuraCacheEntry cached = GUARDIAN_AURA_CACHE.get(victim.getUUID());
        if (cached != null && cached.tick == tick) {
            return cached.protectedByAura;
        }
        if (GUARDIAN_AURA_CACHE.size() > 512) {
            GUARDIAN_AURA_CACHE.clear();
        }
        int range = GemsBalance.v().sentinel().guardianAuraRadiusBlocks();
        AABB searchBox = victim.getBoundingBox().inflate(range);

        List<ServerPlayer> nearbyPlayers = world.getEntitiesOfClass(
            ServerPlayer.class,
            searchBox,
            p -> p != victim && p.isAlive() && GemPowers.isPassiveActive(p, PowerIds.SENTINEL_GUARDIAN_AURA)
        );

        for (ServerPlayer sentinel : nearbyPlayers) {
            if (GemTrust.isTrusted(sentinel, victim)) {
                GUARDIAN_AURA_CACHE.put(victim.getUUID(), new GuardianAuraCacheEntry(tick, true));
                return true;
            }
        }
        GUARDIAN_AURA_CACHE.put(victim.getUUID(), new GuardianAuraCacheEntry(tick, false));
        return false;
    }

    public static float getGuardianAuraDamageReduction() {
        return GemsBalance.v().sentinel().guardianAuraDamageReduction();
    }

    // ========== Fortress ==========
    // While standing still for 2s, gain Resistance II.

    public static void tickFortress(ServerPlayer player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SENTINEL_FORTRESS)) {
            clearFortress(player);
            return;
        }

        BlockPos currentPos = player.blockPosition();
        String storedPosStr = PlayerStateManager.getPersistent(player, FORTRESS_POS_KEY);
        String startStr = PlayerStateManager.getPersistent(player, FORTRESS_START_KEY);

        String currentPosStr = currentPos.getX() + "," + currentPos.getY() + "," + currentPos.getZ();

        if (storedPosStr == null || !storedPosStr.equals(currentPosStr)) {
            // Player moved - reset timer
            PlayerStateManager.setPersistent(player, FORTRESS_POS_KEY, currentPosStr);
            PlayerStateManager.setPersistent(player, FORTRESS_START_KEY, String.valueOf(player.level().getGameTime()));
            return;
        }

        if (startStr == null) {
            PlayerStateManager.setPersistent(player, FORTRESS_START_KEY, String.valueOf(player.level().getGameTime()));
            return;
        }

        long startTime = Long.parseLong(startStr);
        int requiredTicks = GemsBalance.v().sentinel().fortressStandStillTicks();
        
        if (player.level().getGameTime() - startTime >= requiredTicks) {
            int amplifier = GemsBalance.v().sentinel().fortressResistanceAmplifier();
            int duration = 200; // 10 seconds - persists after movement
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amplifier, false, false));
        }
    }

    public static void clearFortress(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, FORTRESS_POS_KEY);
        PlayerStateManager.clearPersistent(player, FORTRESS_START_KEY);
    }

    // ========== Retribution Thorns ==========
    // Attackers take 20% of damage dealt back as true damage.

    public static float getRetributionThornsDamagePercent() {
        return GemsBalance.v().sentinel().retributionThornsDamageMultiplier();
    }

    public static boolean hasRetributionThorns(ServerPlayer player) {
        return GemPowers.isPassiveActive(player, PowerIds.SENTINEL_RETRIBUTION_THORNS);
    }

    private record GuardianAuraCacheEntry(long tick, boolean protectedByAura) {}
}
