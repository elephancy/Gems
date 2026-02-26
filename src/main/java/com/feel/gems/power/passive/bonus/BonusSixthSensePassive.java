package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sixth Sense - Warning particle effect when enemy targets you.
 * Implementation via tick handler that checks for threats.
 */
public final class BonusSixthSensePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SIXTH_SENSE;
    }

    @Override
    public String name() {
        return "Sixth Sense";
    }

    @Override
    public String description() {
        return "Warning particles appear when an enemy is targeting you from behind.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - implemented via tick handler
    }

    @Override
    public void remove(ServerPlayer player) {
        // Marker passive
    }
}
