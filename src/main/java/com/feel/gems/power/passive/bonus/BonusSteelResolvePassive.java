package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Steel Resolve - Immune to knockback.
 * Implementation via knockback event hook.
 */
public final class BonusSteelResolvePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_STEEL_RESOLVE;
    }

    @Override
    public String name() {
        return "Steel Resolve";
    }

    @Override
    public String description() {
        return "Immune to knockback from attacks.";
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
