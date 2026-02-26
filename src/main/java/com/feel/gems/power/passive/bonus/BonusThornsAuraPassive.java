package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Thorns Aura - Reflects damage back to attackers.
 * Implementation via mixin/event hook checking for this passive.
 */
public final class BonusThornsAuraPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_THORNS_AURA;
    }

    @Override
    public String name() {
        return "Thorns Aura";
    }

    @Override
    public String description() {
        return "Reflect 30% of incoming melee damage back to attackers.";
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
