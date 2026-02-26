package com.feel.gems.power.gem.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

public final class HunterPreyMarkRuntime {
    private static final String MARK_TARGET_KEY = "hunter_prey_mark_target";
    private static final String MARK_END_KEY = "hunter_prey_mark_end";

    private HunterPreyMarkRuntime() {}

    public static void applyMark(ServerPlayer hunter, LivingEntity target) {
        if (!GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_PREY_MARK)) {
            return;
        }

        int durationTicks = GemsBalance.v().hunter().preyMarkDurationTicks();
        long endTime = hunter.level().getGameTime() + durationTicks;

        PlayerStateManager.setPersistent(hunter, MARK_TARGET_KEY, target.getStringUUID());
        PlayerStateManager.setPersistent(hunter, MARK_END_KEY, String.valueOf(endTime));

        // Apply glowing to target if tracker's eye is active
        if (GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_TRACKERS_EYE)) {
            int range = GemsBalance.v().hunter().trackersEyeRangeBlocks();
            if (range <= 0 || hunter.distanceToSqr(target) <= (double) range * range) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0, false, false));
            }
        }
    }

    public static LivingEntity getMarkedTarget(ServerPlayer hunter) {
        String targetStr = PlayerStateManager.getPersistent(hunter, MARK_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(hunter, MARK_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (hunter.level().getGameTime() > endTime) {
            clearMark(hunter);
            return null;
        }

        try {
            UUID targetId = UUID.fromString(targetStr);
            MinecraftServer server = hunter.level().getServer();
            if (server == null) return null;
            var entity = SummonerSummons.findEntity(server, targetId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                return null;
            }
            return living;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isMarked(ServerPlayer hunter, LivingEntity target) {
        LivingEntity marked = getMarkedTarget(hunter);
        return marked != null && marked.getUUID().equals(target.getUUID());
    }

    public static float getDamageMultiplier() {
        return GemsBalance.v().hunter().preyMarkBonusDamageMultiplier();
    }

    public static void tickTrackersEye(ServerPlayer hunter) {
        if (!GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_TRACKERS_EYE)) {
            return;
        }
        LivingEntity marked = getMarkedTarget(hunter);
        if (marked == null) {
            return;
        }
        if (marked instanceof ServerPlayer markedPlayer
                && GemPowers.isPassiveActive(markedPlayer, PowerIds.SPY_FALSE_SIGNATURE)) {
            return;
        }
        int range = GemsBalance.v().hunter().trackersEyeRangeBlocks();
        if (range > 0 && hunter.distanceToSqr(marked) > (double) range * range) {
            return;
        }
        marked.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
    }

    public static void clearMark(ServerPlayer hunter) {
        PlayerStateManager.clearPersistent(hunter, MARK_TARGET_KEY);
        PlayerStateManager.clearPersistent(hunter, MARK_END_KEY);
    }
}
