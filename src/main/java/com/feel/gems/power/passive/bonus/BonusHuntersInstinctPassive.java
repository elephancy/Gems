package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hunter's Instinct - Crit chance increases against fleeing enemies.
 * Implementation via attack event hook that checks target movement.
 */
public final class BonusHuntersInstinctPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_HUNTERS_INSTINCT;
    }

    @Override
    public String name() {
        return "Hunter's Instinct";
    }

    @Override
    public String description() {
        return "Gain 50% crit chance against enemies moving away from you.";
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
