package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Treasure Hunter - Increased rare drop chance from mobs.
 * Implementation via loot modifier or event hook.
 */
public final class BonusTreasureHunterPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_TREASURE_HUNTER;
    }

    @Override
    public String name() {
        return "Treasure Hunter";
    }

    @Override
    public String description() {
        return "Mobs have an increased chance to drop rare items.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - implemented via loot event hooks
    }

    @Override
    public void remove(ServerPlayer player) {
        // Marker passive
    }
}
