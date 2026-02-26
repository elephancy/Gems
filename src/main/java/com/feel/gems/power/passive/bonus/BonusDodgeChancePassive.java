package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Dodge Chance - Small chance to completely avoid damage.
 * Implementation via mixin/event hook checking for this passive.
 */
public final class BonusDodgeChancePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_DODGE_CHANCE;
    }

    @Override
    public String name() {
        return "Dodge Chance";
    }

    @Override
    public String description() {
        return "10% chance to completely dodge incoming attacks.";
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
