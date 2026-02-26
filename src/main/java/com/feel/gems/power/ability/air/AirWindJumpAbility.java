package com.feel.gems.power.ability.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;




public final class AirWindJumpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_WIND_JUMP;
    }

    @Override
    public String name() {
        return "Wind Jump";
    }

    @Override
    public String description() {
        return "Launches you upward like a wind charge.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().windJumpCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        double up = GemsBalance.v().air().windJumpVerticalVelocity();
        double forward = GemsBalance.v().air().windJumpForwardVelocity();
        if (up <= 0.0D && forward <= 0.0D) {
            return false;
        }
        Vec3 dir = player.getViewVector(1.0F).normalize();
        player.push(dir.x * forward, up, dir.z * forward);
        player.needsSync = true;
        player.fallDistance = 0.0F;
        AbilityFeedback.syncVelocity(player);
        AbilityFeedback.burst(player, ParticleTypes.GUST, 18, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 1.2F);
        return true;
    }
}

