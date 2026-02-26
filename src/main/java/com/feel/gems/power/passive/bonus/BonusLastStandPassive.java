package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Last Stand - Deal 50% more damage when below 25% health.
 * Implementation via damage event hook.
 */
public final class BonusLastStandPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_LAST_STAND;
    }

    @Override
    public String name() {
        return "Last Stand";
    }

    @Override
    public String description() {
        return "Deal 50% more damage when below 25% health.";
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
