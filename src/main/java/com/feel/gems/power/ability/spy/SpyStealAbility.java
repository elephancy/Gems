package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.feel.gems.power.gem.spy.SpySystem;


public final class SpyStealAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_STEAL;
    }

    @Override
    public String name() {
        return "Steal";
    }

    @Override
    public String description() {
        return "Spend 4 observations to steal the selected ability (consumes the observations).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().stealCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        return SpySystem.stealLastSeen(player);
    }
}
