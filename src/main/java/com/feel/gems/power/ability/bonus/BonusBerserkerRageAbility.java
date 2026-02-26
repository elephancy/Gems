package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BonusBerserkerRageAbility implements GemAbility {
    private static final String BERSERKER_RAGE_UNTIL = "bonus_berserker_rage_until";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BERSERKER_RAGE;
    }

    @Override
    public String name() {
        return "Berserker Rage";
    }

    @Override
    public String description() {
        return "Gain massive damage but take increased damage for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().berserkerRageCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        ServerLevel world = player.level();
        int durationTicks = GemsBalance.v().bonusPool().berserkerRageDurationSeconds * 20;
        if (durationTicks <= 0) {
            return false;
        }
        long until = world.getGameTime() + durationTicks;
        PlayerStateManager.setPersistent(player, BERSERKER_RAGE_UNTIL, String.valueOf(until));

        world.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.1);
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        String untilStr = PlayerStateManager.getPersistent(player, BERSERKER_RAGE_UNTIL);
        if (untilStr == null || untilStr.isEmpty()) {
            return false;
        }
        long until = Long.parseLong(untilStr);
        if (player.level().getGameTime() > until) {
            PlayerStateManager.clearPersistent(player, BERSERKER_RAGE_UNTIL);
            return false;
        }
        return true;
    }

    public static float getDamageBoostMultiplier() {
        return 1.0f + (GemsBalance.v().bonusPool().berserkerRageDamageBoostPercent / 100.0f);
    }

    public static float getDamageTakenMultiplier() {
        return 1.0f + (GemsBalance.v().bonusPool().berserkerRageDamageTakenBoostPercent / 100.0f);
    }
}
