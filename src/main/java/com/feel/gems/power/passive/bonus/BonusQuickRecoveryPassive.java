package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Quick Recovery - Reduce all debuff durations by 30%.
 * Implementation via effect application hook.
 */
public final class BonusQuickRecoveryPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_QUICK_RECOVERY;
    }

    @Override
    public String name() {
        return "Quick Recovery";
    }

    @Override
    public String description() {
        return "Negative status effects have 30% reduced duration.";
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
