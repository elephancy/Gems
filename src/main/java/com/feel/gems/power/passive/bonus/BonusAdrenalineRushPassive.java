package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Siphon Soul - Gain regeneration on kill.
 * Implementation via kill event hook.
 */
public final class BonusAdrenalineRushPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ADRENALINE_RUSH;
    }

    @Override
    public String name() {
        return "Siphon Soul";
    }

    @Override
    public String description() {
        return "Killing blows grant Regeneration for a short time.";
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
