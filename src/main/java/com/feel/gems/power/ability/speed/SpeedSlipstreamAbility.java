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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import com.feel.gems.power.gem.speed.SpeedMomentum;


public final class SpeedSlipstreamAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_SLIPSTREAM;
    }

    @Override
    public String name() {
        return "Slipstream";
    }

    @Override
    public String description() {
        return "Creates a wind lane ahead that boosts trusted allies and hinders enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().slipstreamCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        float momentum = SpeedMomentum.multiplier(player);
        int baseDuration = Math.max(1, Math.round(GemsBalance.v().speed().slipstreamDurationTicks() * momentum));
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPEED, baseDuration);
        Vec3 dir = player.getViewVector(1.0F);

        AbilityRuntime.startSpeedSlipstream(player, dir, duration, momentum);
        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 1.3F);
        if (!(player.level() instanceof ServerLevel world)) {
            player.sendSystemMessage(Component.translatable("gems.ability.speed.slipstream.active"), true);
            return true;
        }
        AbilityFeedback.beam(
                world,
                player.position().add(0.0D, 0.8D, 0.0D),
                player.position().add(dir.x * 6.0D, 0.8D, dir.z * 6.0D),
                ParticleTypes.CLOUD,
                12
        );
        player.sendSystemMessage(Component.translatable("gems.ability.speed.slipstream.active"), true);
        return true;
    }
}
