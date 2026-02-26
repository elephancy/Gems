package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Chain Breaker - Automatically break free from roots/slows faster.
 * Implementation via status effect tick handler.
 */
public final class BonusChainBreakerPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CHAIN_BREAKER;
    }

    @Override
    public String name() {
        return "Chain Breaker";
    }

    @Override
    public String description() {
        return "Movement-impairing effects have 50% reduced duration.";
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
