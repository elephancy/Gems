package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public final class DuelistBladeDanceAbility implements GemAbility {
    public static final String BLADE_DANCE_ACTIVE_KEY = "duelist_blade_dance_active";
    public static final String BLADE_DANCE_STACKS_KEY = "duelist_blade_dance_stacks";
    public static final String BLADE_DANCE_LAST_HIT_KEY = "duelist_blade_dance_last_hit";
    public static final String BLADE_DANCE_END_KEY = "duelist_blade_dance_end";

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_BLADE_DANCE;
    }

    @Override
    public String name() {
        return "Blade Dance";
    }

    @Override
    public String description() {
        return "Combo system where consecutive hits deal increasing damage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().bladeDanceCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.DUELIST, GemsBalance.v().duelist().bladeDanceDurationTicks());
        long endTime = player.level().getGameTime() + durationTicks;

        PlayerStateManager.setPersistent(player, BLADE_DANCE_ACTIVE_KEY, "true");
        PlayerStateManager.setPersistent(player, BLADE_DANCE_STACKS_KEY, "0");
        PlayerStateManager.setPersistent(player, BLADE_DANCE_LAST_HIT_KEY, String.valueOf(player.level().getGameTime()));
        PlayerStateManager.setPersistent(player, BLADE_DANCE_END_KEY, String.valueOf(endTime));

        AbilityFeedback.burstAt(player.level(), player.position().add(0.0D, 1.0D, 0.0D),
                ParticleTypes.CRIT, 25, 0.5D);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.5F);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        String active = PlayerStateManager.getPersistent(player, BLADE_DANCE_ACTIVE_KEY);
        if (!"true".equals(active)) {
            return false;
        }
        String endStr = PlayerStateManager.getPersistent(player, BLADE_DANCE_END_KEY);
        if (endStr == null) {
            return false;
        }
        long endTime = Long.parseLong(endStr);
        return player.level().getGameTime() < endTime;
    }

    public static float getDamageMultiplier(ServerPlayer player) {
        if (!isActive(player)) {
            return 1.0F;
        }

        // Check if combo has expired
        String lastHitStr = PlayerStateManager.getPersistent(player, BLADE_DANCE_LAST_HIT_KEY);
        if (lastHitStr != null) {
            long lastHit = Long.parseLong(lastHitStr);
            int resetTicks = GemsBalance.v().duelist().bladeDanceResetTicks();
            if (player.level().getGameTime() - lastHit > resetTicks) {
                // Reset stacks
                PlayerStateManager.setPersistent(player, BLADE_DANCE_STACKS_KEY, "0");
            }
        }

        String stacksStr = PlayerStateManager.getPersistent(player, BLADE_DANCE_STACKS_KEY);
        int stacks = stacksStr != null ? Integer.parseInt(stacksStr) : 0;

        float baseMultiplier = GemsBalance.v().duelist().bladeDanceStartingMultiplier();
        float increasePerHit = GemsBalance.v().duelist().bladeDanceIncreasePerHit();
        float maxMultiplier = GemsBalance.v().duelist().bladeDanceMaxMultiplier();

        return Math.min(baseMultiplier + (stacks * increasePerHit), maxMultiplier);
    }

    public static void onHit(ServerPlayer player) {
        if (!isActive(player)) {
            return;
        }

        String stacksStr = PlayerStateManager.getPersistent(player, BLADE_DANCE_STACKS_KEY);
        int stacks = stacksStr != null ? Integer.parseInt(stacksStr) : 0;
        stacks++;

        PlayerStateManager.setPersistent(player, BLADE_DANCE_STACKS_KEY, String.valueOf(stacks));
        PlayerStateManager.setPersistent(player, BLADE_DANCE_LAST_HIT_KEY, String.valueOf(player.level().getGameTime()));

        // Visual feedback for stacks
        if (stacks % 3 == 0) {
            AbilityFeedback.burstAt(player.level(), player.position().add(0.0D, 1.5D, 0.0D),
                    ParticleTypes.CRIT, 10, 0.3D);
        }
    }

    public static void clear(ServerPlayer player) {
        PlayerStateManager.clearPersistent(player, BLADE_DANCE_ACTIVE_KEY);
        PlayerStateManager.clearPersistent(player, BLADE_DANCE_STACKS_KEY);
        PlayerStateManager.clearPersistent(player, BLADE_DANCE_LAST_HIT_KEY);
        PlayerStateManager.clearPersistent(player, BLADE_DANCE_END_KEY);
    }
}
