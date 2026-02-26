package com.feel.gems.power.ability.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.feel.gems.power.gem.speed.SpeedMomentum;


public final class TerminalVelocityAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERMINAL_VELOCITY;
    }

    @Override
    public String name() {
        return "Terminal Velocity";
    }

    @Override
    public String description() {
        return "Temporarily grants Speed V and Haste III.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().terminalVelocityCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        float momentum = SpeedMomentum.multiplier(player);
        int baseDuration = Math.max(1, Math.round(GemsBalance.v().speed().terminalVelocityDurationTicks() * momentum));
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPEED, baseDuration);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, GemsBalance.v().speed().terminalVelocitySpeedAmplifier(), true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, GemsBalance.v().speed().terminalVelocityHasteAmplifier(), true, false, false));
        AbilityFeedback.sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9F, 1.3F);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 14, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.speed.terminal_velocity.active"), true);
        return true;
    }
}
