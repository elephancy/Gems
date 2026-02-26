package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Arcane Barrier - Absorb first hit every 30 seconds.
 * Implementation via damage event hook with cooldown tracking.
 */
public final class BonusArcaneBarrierPassive implements GemPassive {
    public static final int BARRIER_COOLDOWN = 600; // 30 seconds in ticks

    @Override
    public Identifier id() {
        return PowerIds.BONUS_ARCANE_BARRIER;
    }

    @Override
    public String name() {
        return "Arcane Barrier";
    }

    @Override
    public String description() {
        return "Absorb the first hit every 30 seconds completely.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Set barrier as ready
        player.removeTag("gems_arcane_barrier_cooldown");
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeTag("gems_arcane_barrier_cooldown");
    }
}
