package com.feel.gems.power.ability.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.air.AirGaleSlamRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;




public final class AirGaleSlamAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_GALE_SLAM;
    }

    @Override
    public String name() {
        return "Gale Slam";
    }

    @Override
    public String description() {
        return "Empowers your next mace hit to create a stronger wind slam.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().galeSlamCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int window = GemsBalance.v().air().galeSlamWindowTicks();
        if (window <= 0) {
            return false;
        }
        AirGaleSlamRuntime.start(player, window);
        AbilityFeedback.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.3F);
        return true;
    }
}

