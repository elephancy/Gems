package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Vengeance - After being hit, next attack deals +50% damage.
 * Implementation via damage receive and attack event hooks.
 */
public final class BonusVengeancePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_VENGEANCE;
    }

    @Override
    public String name() {
        return "Vengeance";
    }

    @Override
    public String description() {
        return "After taking damage, your next attack deals +50% damage.";
    }

    @Override
    public void apply(ServerPlayer player) {
        player.removeTag("gems_vengeance_ready");
    }

    @Override
    public void remove(ServerPlayer player) {
        player.removeTag("gems_vengeance_ready");
    }
}
