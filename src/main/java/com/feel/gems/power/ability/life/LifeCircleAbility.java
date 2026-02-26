package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.particle.ModParticles;
import com.feel.gems.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;



public final class LifeCircleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.LIFE_CIRCLE;
    }

    @Override
    public String name() {
        return "Life Circle";
    }

    @Override
    public String description() {
        return "Aura that boosts trusted players' max health and reduces enemies' max health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().lifeCircleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.LIFE, GemsBalance.v().life().lifeCircleDurationTicks());
        AbilityRuntime.startLifeCircle(player, duration);
        AbilityFeedback.sound(player, ModSounds.ARCANE_CHIME, 0.7F, 0.9F);
        AbilityFeedback.burst(player, ParticleTypes.HEART, 16, 0.35D);
        AbilityFeedback.burst(player, ModParticles.GEM_RING, 5, 0.3D);
        player.sendSystemMessage(Component.translatable("gems.ability.life.life_circle.active"), true);
        return true;
    }
}
