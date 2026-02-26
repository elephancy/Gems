package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Battle Medic - Heal nearby allies slowly in combat.
 * Implementation via tick handler.
 */
public final class BonusBattleMedicPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BATTLE_MEDIC;
    }

    @Override
    public String name() {
        return "Battle Medic";
    }

    @Override
    public String description() {
        return "Nearby allies within 10 blocks slowly regenerate health.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Marker passive - implemented via tick handler
    }

    @Override
    public void remove(ServerPlayer player) {
        // Marker passive
    }
}
