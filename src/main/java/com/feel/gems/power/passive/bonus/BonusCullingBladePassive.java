package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Culling Blade - Deal increased damage to low-health enemies.
 * Implementation via event hook.
 */
public final class BonusCullingBladePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CULLING_BLADE;
    }

    @Override
    public String name() {
        return "Culling Blade";
    }

    @Override
    public String description() {
        return "Deal increased damage to enemies below 25% health.";
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
