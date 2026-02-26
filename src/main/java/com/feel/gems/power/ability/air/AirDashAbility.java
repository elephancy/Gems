package com.feel.gems.power.ability.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;




public final class AirDashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_DASH;
    }

    @Override
    public String name() {
        return "Air Dash";
    }

    @Override
    public String description() {
        return "Dashes forward with a brief i-frame window.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().dashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        double velocity = GemsBalance.v().air().dashVelocity();
        double up = GemsBalance.v().air().dashUpVelocity();
        int iFrame = AugmentRuntime.applyDurationMultiplier(player, GemId.AIR, GemsBalance.v().air().dashIFrameDurationTicks());
        int amp = GemsBalance.v().air().dashIFrameResistanceAmplifier();
        if (velocity <= 0.0D) {
            return false;
        }

        Vec3 dir = player.getViewVector(1.0F).normalize();
        player.push(dir.x * velocity, up, dir.z * velocity);
        player.needsSync = true;
        AbilityFeedback.syncVelocity(player);
        if (iFrame > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, iFrame, amp, true, false, false));
        }

        AbilityFeedback.beam(player.level(),
                player.position().add(0.0D, 1.0D, 0.0D),
                player.position().add(dir.scale(3.0D)).add(0.0D, 1.0D, 0.0D),
                ParticleTypes.CLOUD,
                12);
        AbilityFeedback.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.7F, 1.4F);
        return true;
    }
}

