package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Second Wind - Once per life, survive a killing blow with 1 HP.
 * Implementation via damage event hook.
 */
public final class BonusSecondWindPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SECOND_WIND;
    }

    @Override
    public String name() {
        return "Second Wind";
    }

    @Override
    public String description() {
        return "Once per life, survive a killing blow with 1 HP.";
    }

    @Override
    public void apply(ServerPlayer player) {
        // Reset second wind availability
        player.removeTag("gems_second_wind_used");
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeTag("gems_second_wind_used");
    }
}
