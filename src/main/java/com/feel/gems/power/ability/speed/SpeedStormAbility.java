package com.feel.gems.power.ability.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import com.feel.gems.power.gem.speed.SpeedMomentum;


public final class SpeedStormAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_STORM;
    }

    @Override
    public String name() {
        return "Speed Storm";
    }

    @Override
    public String description() {
        return "Creates a field that buffs trusted players and heavily slows enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().speedStormCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        float momentum = SpeedMomentum.multiplier(player);
        int baseDuration = Math.max(1, Math.round(GemsBalance.v().speed().speedStormDurationTicks() * momentum));
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPEED, baseDuration);
        AbilityRuntime.startSpeedStorm(player, duration, momentum);
        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 20, 0.45D);
        player.sendSystemMessage(Component.translatable("gems.ability.speed.storm.active"), true);
        return true;
    }
}
