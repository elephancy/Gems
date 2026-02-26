package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Unbreakable - Equipment durability loss reduced by 50%.
 * Implementation via item damage event hook.
 */
public final class BonusUnbreakablePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_UNBREAKABLE;
    }

    @Override
    public String name() {
        return "Unbreakable";
    }

    @Override
    public String description() {
        return "Equipment loses durability 50% slower.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - implemented via item damage event hooks
    }

    @Override
    public void remove(ServerPlayer player) {
        // Marker passive
    }
}
