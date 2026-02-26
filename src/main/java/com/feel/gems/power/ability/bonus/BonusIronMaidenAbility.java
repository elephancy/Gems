package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Iron Maiden - attackers take reflected damage for a short duration.
 */
public final class BonusIronMaidenAbility implements GemAbility {
    private static final String IRON_MAIDEN_UNTIL = "bonus_iron_maiden_until";

    @Override
    public Identifier id() {
        return PowerIds.BONUS_IRON_MAIDEN;
    }

    @Override
    public String name() {
        return "Iron Maiden";
    }

    @Override
    public String description() {
        return "Enemies that hit you take reflected damage for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().ironMaidenCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = GemsBalance.v().bonusPool().ironMaidenDurationSeconds * 20;
        if (duration <= 0) {
            return false;
        }
        long until = player.level().getGameTime() + duration;
        PlayerStateManager.setPersistent(player, IRON_MAIDEN_UNTIL, String.valueOf(until));
        return true;
    }

    public static boolean isActive(ServerPlayer player) {
        String untilStr = PlayerStateManager.getPersistent(player, IRON_MAIDEN_UNTIL);
        if (untilStr == null || untilStr.isEmpty()) {
            return false;
        }
        long until = Long.parseLong(untilStr);
        if (player.level().getGameTime() > until) {
            PlayerStateManager.clearPersistent(player, IRON_MAIDEN_UNTIL);
            return false;
        }
        return true;
    }
}
