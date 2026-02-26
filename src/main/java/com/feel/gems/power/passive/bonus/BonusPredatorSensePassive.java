package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Predator Sense - Glowing effect on low-health enemies.
 * Implementation via tick handler that checks nearby entities.
 */
public final class BonusPredatorSensePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_PREDATOR_SENSE;
    }

    @Override
    public String name() {
        return "Predator Sense";
    }

    @Override
    public String description() {
        return "Enemies below 30% HP glow, revealing their location.";
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
