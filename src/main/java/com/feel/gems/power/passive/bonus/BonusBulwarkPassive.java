package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Bulwark - Blocking is 50% more effective.
 * Implementation via damage event hook when blocking.
 */
public final class BonusBulwarkPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BULWARK;
    }

    @Override
    public String name() {
        return "Bulwark";
    }

    @Override
    public String description() {
        return "Shield blocking absorbs 50% more damage.";
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
