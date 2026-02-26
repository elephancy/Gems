package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Nemesis - Deal 25% more damage to the last player who killed you.
 * Implementation via death event (store killer) and damage event hooks.
 */
public final class BonusNemesisPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_NEMESIS;
    }

    @Override
    public String name() {
        return "Nemesis";
    }

    @Override
    public String description() {
        return "Deal 25% more damage to the last player who killed you.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - uses persistent data for nemesis tracking
    }

    @Override
    public void remove(ServerPlayer player) {
        // Keep nemesis data even when passive removed
    }
}
