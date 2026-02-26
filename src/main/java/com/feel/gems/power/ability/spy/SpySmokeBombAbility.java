package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import com.feel.gems.power.gem.spy.SpySystem;


public final class SpySmokeBombAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_SMOKE_BOMB;
    }

    @Override
    public String name() {
        return "Smoke Bomb";
    }

    @Override
    public String description() {
        return "Blinds and slows nearby untrusted players, and briefly cloaks you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spy().smokeBombCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel world)) {
            return false;
        }
        int radius = GemsBalance.v().spy().smokeBombRadiusBlocks();
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.SPY, GemsBalance.v().spy().smokeBombDurationTicks());
        if (radius <= 0 || duration <= 0) {
            return false;
        }
        int blindAmp = GemsBalance.v().spy().smokeBombBlindnessAmplifier();
        int slowAmp = GemsBalance.v().spy().smokeBombSlownessAmplifier();

        AABB box = new AABB(player.blockPosition()).inflate(radius);
        for (LivingEntity other : world.getEntitiesOfClass(LivingEntity.class, box, p -> p != player)) {
            if (other instanceof ServerPlayer otherPlayer && !SpySystem.canAffect(player, otherPlayer)) {
                continue;
            }
            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, blindAmp, true, false, false));
            other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, slowAmp, true, false, false));
        }

        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.min(duration, 40), 0, true, false, false));
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 28, 0.45D);
        AbilityFeedback.sound(player, SoundEvents.PUFFER_FISH_BLOW_UP, 0.6F, 0.7F);
        return true;
    }
}

