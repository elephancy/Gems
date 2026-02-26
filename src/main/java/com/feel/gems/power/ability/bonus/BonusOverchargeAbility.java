package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Overcharge - Next ability deals double damage but costs health.
 * Implementation uses a temporary player state flag consumed on the next damage event.
 */
public final class BonusOverchargeAbility implements GemAbility {
    private static final String OVERCHARGE_UNTIL_KEY = "bonus_overcharge_until";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_OVERCHARGE;
    }

    @Override
    public String name() {
        return "Overcharge";
    }

    @Override
    public String description() {
        return "Your next ability deals double damage but costs health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().overchargeCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) return false;
        var cfg = GemsBalance.v().bonusPool();
        float healthCost = cfg.overchargeHealthCost;
        if (healthCost <= 0.0f) {
            return false;
        }
        if (player.getHealth() <= healthCost) {
            return false; // Can't afford health cost
        }
        if (isActive(player)) {
            return false;
        }

        // Pay health cost
        player.hurtServer(world, world.damageSources().magic(), healthCost);

        // Mark player for overcharge bonus
        int durationTicks = cfg.overchargeDurationSeconds * 20;
        long until = player.level().getGameTime() + Math.max(0, durationTicks);
        PlayerStateManager.setPersistent(player, OVERCHARGE_UNTIL_KEY, String.valueOf(until));

        // Energy buildup particles
        for (int i = 0; i < 30; i++) {
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double radius = world.getRandom().nextDouble() * 1.5;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.getRandom().nextDouble() * 2;
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0.2, 0, 0.05);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 2.0f);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        String untilStr = PlayerStateManager.getPersistent(player, OVERCHARGE_UNTIL_KEY);
        if (untilStr == null || untilStr.isEmpty()) {
            return false;
        }
        long until = Long.parseLong(untilStr);
        if (player.level().getGameTime() > until) {
            PlayerStateManager.clearPersistent(player, OVERCHARGE_UNTIL_KEY);
            return false;
        }
        return true;
    }

    public static float consumeDamageMultiplier(ServerPlayer player) {
        if (!isActive(player)) {
            return 1.0f;
        }
        PlayerStateManager.clearPersistent(player, OVERCHARGE_UNTIL_KEY);
        return GemsBalance.v().bonusPool().overchargeDamageMultiplier;
    }
}
