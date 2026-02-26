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


public final class SpeedAfterimageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_AFTERIMAGE;
    }

    @Override
    public String name() {
        return "Afterimage";
    }

    @Override
    public String description() {
        return "Briefly vanish with a speed burst; the first hit breaks the effect.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().afterimageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        float momentum = SpeedMomentum.multiplier(player);
        int baseDuration = Math.max(1, Math.round(GemsBalance.v().speed().afterimageDurationTicks() * momentum));
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPEED, baseDuration);
        AbilityRuntime.startSpeedAfterimage(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.7F, 1.5F);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 14, 0.35D);
        player.sendSystemMessage(Component.translatable("gems.ability.speed.afterimage.active"), true);
        return true;
    }
}
