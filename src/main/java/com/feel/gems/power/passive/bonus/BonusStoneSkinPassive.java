package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Stone Skin - Reduce all incoming damage by flat 1 HP.
 * Implementation via damage event hook.
 */
public final class BonusStoneSkinPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_STONE_SKIN;
    }

    @Override
    public String name() {
        return "Stone Skin";
    }

    @Override
    public String description() {
        return "Reduce all incoming damage by 1 HP (flat reduction).";
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
