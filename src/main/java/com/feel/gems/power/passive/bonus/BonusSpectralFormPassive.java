package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mist Form - Chance to phase through attacks.
 * Implementation via damage hook.
 */
public final class BonusSpectralFormPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_FORM;
    }

    @Override
    public String name() {
        return "Mist Form";
    }

    @Override
    public String description() {
        return "10% chance to phase through attacks.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - implemented via damage hooks
    }

    @Override
    public void remove(ServerPlayer player) {
        // Marker passive
    }
}
