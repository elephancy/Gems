package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mana Shield - Absorb damage with XP levels.
 * Implementation via mixin/event hook checking for this passive.
 */
public final class BonusManaShieldPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_MANA_SHIELD;
    }

    @Override
    public String name() {
        return "Mana Shield";
    }

    @Override
    public String description() {
        return "Absorb 25% of incoming damage using XP levels.";
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
