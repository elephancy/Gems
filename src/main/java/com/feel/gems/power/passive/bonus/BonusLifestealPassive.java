package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lifesteal - Heal a portion of damage dealt.
 * Implementation via mixin/event hook checking for this passive.
 */
public final class BonusLifestealPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_LIFESTEAL;
    }

    @Override
    public String name() {
        return "Lifesteal";
    }

    @Override
    public String description() {
        return "Heal 15% of all melee damage dealt.";
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
