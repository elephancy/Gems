package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Intimidate - Intimidate nearby enemies, reducing their effectiveness.
 * Implementation via event hook.
 */
public final class BonusIntimidatePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_INTIMIDATE;
    }

    @Override
    public String name() {
        return "Intimidate";
    }

    @Override
    public String description() {
        return "Nearby enemies deal reduced damage and have slower attack speed.";
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
