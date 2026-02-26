package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * XP Boost - Gain bonus XP from all sources.
 * Implementation via event hook.
 */
public final class BonusXpBoostPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_XP_BOOST;
    }

    @Override
    public String name() {
        return "XP Boost";
    }

    @Override
    public String description() {
        return "Gain 25% bonus XP from all sources.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - implemented via event hooks
    }

    @Override
    public void remove(ServerPlayer player) {
        // Marker passive
    }
}
